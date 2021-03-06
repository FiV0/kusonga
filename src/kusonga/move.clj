;; Copyright (c) Stuart Sierra, 2012. All rights reserved. The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution. By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license. You must not
;; remove this notice, or any other, from this software.

(ns ^{:author "Stuart Sierra, Benedek Fazekas"
      :doc    "Refactoring tool to move a Clojure namespace from one name/file to
  another, and update all references to that namespace in your other
  Clojure source files.

  WARNING: This code is ALPHA and subject to change. It also modifies
  and deletes your source files! Make sure you have a backup or
  version control.

  DISCLAIMER:
  This is a heavily modified version of Stuart Sierra's original clojure.tools.namespace.move "}
    kusonga.move
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.namespace.find :as find]
            [kusonga.util :as util]
            [parallel.core :as p]
            [rewrite-clj.parser :as parser]
            [rewrite-clj.reader :as reader]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.base :as b])
  (:import (java.io File FileNotFoundException)))

(defn- update-file
  "Reads file as a string, calls f on the string plus any args, then
  writes out return value of f as the new contents of file. Does not
  modify file if the content is unchanged."
  [file f & args]
  (let [old (slurp file)
        new (str (apply f old args))]
    (when-not (= old new)
      (spit file new))))

(defn- sym->file
  [path sym extension]
  (io/file path (str (util/sym->file-name sym) extension)))

(defn- update? [file extension-of-moved]
  (let [file-ext (util/file->extension file)
        all-extensions #{".cljc" ".cljs" ".clj"}]
    (or
     (and (= ".cljc" extension-of-moved)
          (all-extensions file-ext))
     (= file-ext extension-of-moved)
     (#{".cljc" ".edn"} file-ext))))

(defn- clojure-source-files [dirs extension]
  (->> dirs
       (map io/file)
       (filter #(.exists ^File %))
       (mapcat file-seq)
       (filter (fn [^File file]
                 (and (.isFile file)
                      (update? (str file) extension))))
       (map #(.getCanonicalFile ^File %))))

(defn- prefix-libspec [libspec]
  (let [prefix (str/join "." (butlast (str/split (name libspec) #"\.")))]
    (and prefix (symbol prefix))))

(defn- java-package [sym]
  (str/replace (name sym) "-" "_"))

(defn- java-style-prefix?
  [old-sym node]
  (when-not (#{:uneval} (b/tag node))
    (when-let [node-sexpr (b/sexpr node)]
      (str/starts-with? node-sexpr (java-package old-sym)))))

(defn- libspec-prefix?
  [node node-sexpr old-sym]
  (let [old-sym-prefix-libspec (prefix-libspec old-sym)
        first-node?            (z/leftmost? node)
        parent-leftmost-node   (z/leftmost (z/up node))
        parent-leftmost-sexpr  (and parent-leftmost-node
                                    (not
                                     (#{:uneval}
                                      (b/tag parent-leftmost-node)))
                                    (b/sexpr parent-leftmost-node))]
    (and first-node?
         (= :require parent-leftmost-sexpr)
         (= node-sexpr old-sym-prefix-libspec))))

(defn- contains-sym? [old-sym node]
  (when-not (#{:uneval} (b/tag node))
    (when-let [node-sexpr (b/sexpr node)]
      (or
       (= node-sexpr old-sym)
       (libspec-prefix? node node-sexpr old-sym)))))

(defn- ->new-node [old-node old-sym new-sym]
  (let [old-prefix (prefix-libspec old-sym)]
    (cond-> old-node

      :always
      (str/replace-first
       (name old-sym)
       (name new-sym))

      (= old-prefix old-node)
      (str/replace-first
       (name old-prefix)
       (name (prefix-libspec new-sym))))))

(defn- replace-in-node [old-sym new-sym old-node]
  (let [new-node (->new-node old-node old-sym new-sym)]
    (cond
      (symbol? old-node) (symbol new-node)

      :else new-node)))

(defn- ns-decl? [node]
  (when-not (#{:uneval} (b/tag node))
    (= 'ns (b/sexpr (z/down node)))))

(def ^:const ^:private ns-form-placeholder (str "ns_" "form_" "placeholder"))

(defn- split-ns-form-ns-body
  "Returns ns form as a rewrite-clj loc and the ns body as string with a place holder for the ns form."
  [content]
  (let [reader     (reader/string-reader content)
        first-form (parser/parse reader)]
    (loop [ns-form-maybe (and first-form (z/edn first-form))]
      (if (ns-decl? ns-form-maybe)
        [ns-form-maybe
         (str/replace content (z/root-string ns-form-maybe) ns-form-placeholder)]
        (if-let [next-form (parser/parse reader)]
          (recur (z/edn next-form))
          [nil content])))))

(defn- import? [node]
  (when-not (#{:uneval} (b/tag node))
    (when-let [node-sexpr (b/sexpr node)]
      (= :import node-sexpr))))

(defn- ->new-import-node [old-sym new-sym old-node]
  (let [new-node (str/replace old-node (java-package old-sym) (java-package new-sym))]
    (cond
      (symbol? old-node) (symbol new-node)

      :else new-node)))

(defn- replace-in-import* [import-loc old-sym new-sym]
  (loop [loc import-loc]
    (if-let [found-node (some-> loc
                                (z/find-next-depth-first (partial java-style-prefix? old-sym))
                                (z/edit (partial ->new-import-node old-sym new-sym)))]
      (recur found-node)
      (z/root loc))))

(defn- replace-in-import [ns-loc old-sym new-sym]
  (if-let [import-loc (some-> (z/find-next-depth-first ns-loc import?)
                              (z/up))]
    (-> (z/replace import-loc (replace-in-import* (z/edn (z/node import-loc)) old-sym new-sym))
        z/root
        z/edn)
    ns-loc))

(defn- replace-in-ns-form [ns-loc old-sym new-sym]
  (loop [loc (replace-in-import ns-loc old-sym new-sym)]
    (if-let [found-node (some-> (z/find-next-depth-first loc (partial contains-sym? old-sym))
                                (z/edit (partial replace-in-node old-sym new-sym)))]
      (recur found-node)
      (z/root-string loc))))

(defn- source-replacement [old-sym new-sym match]
  (let [old-ns-ref      (name old-sym)
        new-ns-ref      (name new-sym)
        old-ns-ref-dot  (str old-ns-ref ".")
        new-ns-ref-dot  (str new-ns-ref ".")
        old-pkg-prefix  (java-package old-sym)
        new-pkg-prefix  (java-package new-sym)
        old-type-prefix (str "^" (java-package old-sym))
        new-type-prefix (str "^" (java-package new-sym))]
    (cond

      (= match old-ns-ref)
      new-ns-ref

      (and (str/starts-with? match old-pkg-prefix)
           (str/includes? match "_"))
      (str/replace match old-pkg-prefix new-pkg-prefix)

      (str/starts-with? match old-type-prefix)
      (str/replace match old-type-prefix new-type-prefix)

      (str/starts-with? match old-ns-ref-dot)
      (str/replace match old-ns-ref-dot new-ns-ref-dot)

      :else
      match)))

(def ^:private symbol-regex
  ;; LispReader.java uses #"[:]?([\D&&[^/]].*/)?([\D&&[^/]][^/]*)" but
  ;; that's too broad; we don't want a whole namespace-qualified symbol,
  ;; just each part individually.
  #"\"?[a-zA-Z0-9$%*+=?!<>^_-]['.a-zA-Z0-9$%*+=?!<>_-]*")

(defn- replace-in-source [source-sans-ns old-sym new-sym]
  (str/replace source-sans-ns symbol-regex (partial source-replacement old-sym new-sym)))

(defn- after-platfrom-marker? [platform node]
  (when-not (#{:uneval} (b/tag node))
    (= platform (b/sexpr (z/left node)))))

(defn- find-and-replace-platform-specific-subforms [platform ns-loc]
  (loop [loc         ns-loc
         found-nodes []]
    (if-let [found-node (z/find-next-depth-first loc (partial after-platfrom-marker? platform))]
      (recur (z/replace found-node (symbol (str (name platform) "_require"))) (conj found-nodes found-node))
      [found-nodes (z/edn (z/root loc))])))

(defn- restore-platform-specific-subforms [platform replaced-nodes ns-form]
  (loop [form             ns-form
         [n & rest-nodes] replaced-nodes]
    (if-not n
      form
      (recur (str/replace-first form (str (name platform) "_require") (z/string n)) rest-nodes))))

(defn replace-ns-symbol
  "Given Clojure source as a file, replaces
  all occurrences of the namespace name old-sym with new-sym and
  returns modified source as a string.

  Splits the source file, parses the ns macro if found to do all the necessary
  transformations. Works on the body of namepsace as text as simpler transformations
  are needed. When done puts the ns form and body back together."
  [content old-sym new-sym extension-of-moved file-ext]
  (let [[ns-loc source-sans-ns] (split-ns-form-ns-body content)
        opposite-platform       (util/platform-comp (util/extension->platform extension-of-moved))
        [replaced-nodes ns-loc] (or (and (= ".cljc" file-ext)
                                         opposite-platform
                                         (find-and-replace-platform-specific-subforms opposite-platform ns-loc))
                                    [[] ns-loc])

        new-ns-form             (future (replace-in-ns-form ns-loc old-sym new-sym))
        new-source-sans-ns      (future (replace-in-source source-sans-ns old-sym new-sym))
        new-ns-form             (if (seq replaced-nodes)
                                  (restore-platform-specific-subforms opposite-platform replaced-nodes @new-ns-form)
                                  @new-ns-form)]
    (or
     (and
      new-ns-form
      (str/replace @new-source-sans-ns ns-form-placeholder new-ns-form))
     @new-source-sans-ns)))

(defn move-ns-file
  "Moves the .clj, .cljc or .cljs source file (found relative
  to source-path) for the namespace named old-sym to a file for a
  namespace named new-sym.

  WARNING: This function moves and deletes your source files! Make
  sure you have a backup or version control."
  ([old-sym new-sym source-path]
   (doseq [ext (util/sym->file-extensions source-path old-sym)]
     (move-ns-file old-sym new-sym ext source-path)))
  ([old-sym new-sym extension source-path]
   (if-let [old-file (sym->file source-path old-sym extension)]
     (let [new-file (sym->file source-path new-sym extension)]
       (.mkdirs (.getParentFile new-file))
       (io/copy old-file new-file)
       (.delete old-file)
       (loop [dir (.getParentFile old-file)]
         (when (empty? (.listFiles dir))
           (.delete dir)
           (recur (.getParentFile dir)))))
     (throw (FileNotFoundException. (format "file for %s not found in %s" old-sym source-path))))))

(defn replace-ns-symbol-in-source-files
  "Replaces all occurrences of the old name with the new name in
  all Clojure source files found in dirs."
  [old-sym new-sym extension dirs]
  (p/pmap
   (fn [file]
     (->> (str file)
          util/file->extension
          (update-file file replace-ns-symbol old-sym new-sym extension)))
   (clojure-source-files dirs extension)))

(defn move-ns
  "Moves the .clj, .cljc or .cljs source file (found relative
  to source-path) for the namespace named old-sym to new-sym and
  replace all occurrences of the old name with the new name in all
  Clojure(script) source files found in dirs.

  This is partly textual transformation. It works on
  namespaces require'd or use'd from a prefix list.

  WARNING: This function modifies and deletes your source files! Make
  sure you have a backup or version control."
  ([old-sym new-sym source-path dirs]
   (doseq [ext (util/sym->file-extensions source-path old-sym)]
     (move-ns old-sym new-sym source-path ext dirs)))
  ([old-sym new-sym source-path extension dirs]
   (move-ns-file old-sym new-sym extension source-path)
   (replace-ns-symbol-in-source-files old-sym new-sym extension dirs)))

(defn rename-prefix
  "Moves the .clj, .cljc or .cljs source files (found relative
  to source-paths in dirs) for namespaces prefixed with old-sym-prefix
  to namespaces with new-sym prefixes and
  replace all occurrences of the old name with the new name in all
  Clojure(script) source files found in dirs.

  WARNING: This function modifies and deletes your source files! Make
  sure you have a backup or version control."
  [old-sym-prefix new-sym-prefix dirs]
  (let [dirs+nss (p/pmap (fn [dir]
                           {:dir dir
                            :nss (->> (concat (find/find-namespaces-in-dir dir find/clj)
                                              (find/find-namespaces-in-dir dir find/cljs))
                                      distinct
                                      (filter #(util/prefix-ns? old-sym-prefix %))
                                      ;; needed as otherwise ns finding happens when things already move
                                      doall)}) dirs)]
    ;; not doing this in parallel as it might mess up things
    (doseq [{:keys [dir nss]} dirs+nss]
      (doseq [ns nss]
        (move-ns ns (util/replace-prefix old-sym-prefix new-sym-prefix ns) dir dirs)))))
