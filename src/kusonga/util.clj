(ns kusonga.util
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.io File]))

(defn sym->file-name
  [sym]
  (-> (name sym)
      (str/replace "-" "_")
      (str/replace "." File/separator)))

(defn sym->file-extensions [src-path sym]
  (let [file-name (sym->file-name sym)]
    (filter #(.exists (io/file src-path (str file-name %)))
            '(".clj" ".cljs" ".cljc"))))

(defn file->extension
  [file]
  (or (re-find #"\.clj[cs]?$" file)
      (re-find #"\.edn" file)))

(defn extension->platform
  [extension-of-moved]
  (some->> (#{".cljs" ".clj"} extension-of-moved)
           rest
           (apply str)
           keyword))

(defn platform-comp [platform]
  (when platform
    (->>  #{platform}
          (set/difference #{:cljs :clj})
          first)))

(defn prefix-ns? [prefix-sym ns-sym]
  (let [prefix-ls (str/split (str prefix-sym) #"\.")]
    (= prefix-ls (take (count prefix-ls) (str/split (str ns-sym) #"\.")))))

(defn replace-prefix [old-prefix-sym new-prefix-sym ns-sym]
  (let [prefix-ls (str/split (str old-prefix-sym) #"\.")]
    (->> (str/split (str ns-sym) #"\.")
         (drop (count prefix-ls))
         (str/join ".")
         (str new-prefix-sym ".")
         symbol)))
