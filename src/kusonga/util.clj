(ns kusonga.util
  (:require [clojure.string :as str]
            [clojure.set :as set])
  (:import [java.io File]))

(defn sym->file-name
  [sym]
  (-> (name sym)
      (str/replace "-" "_")
      (str/replace "." File/separator)))

(defn file->extension
  [file]
  (re-find #"\.clj[cs]?$" file))

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
