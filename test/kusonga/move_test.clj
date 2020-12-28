(ns kusonga.move-test
  (:require [kusonga.move :as sut]
            [clojure.test :as t]
            [clojure.java.io :as io])
  (:import [java.io File]))

(def ex-1
  "(ns example.one
  (:require [example.two :as two]
            [example.three :as three]))

(defn foo [m]
  (:example.a.four/bar m)
  (example.a.four/foo))")

(def ex-2
  "(ns
  ^{:added \"0.0.1\"}
  example.two
  (:require [example.three :as three]
            [example.a.four :as four]
            [example.a
             [foo]
             [bar]])
  (:import [example.a.four FourType]
           example.a.four.FooType))

(defn foo []
  (example.a.four/foo))

(defn cljs-foo
  \"This is valid in cljs i am told.\"
  []
  (example.a.four.foo))

(def delayed-four
  (do
    (require 'example.a.four)
    (resolve 'example.a.four/foo)))

(defn my-four-type
  ^example.a.four.FourType
  [^example.a.four.FourType t]
  t)")

(def ex-3
  "(ns example.three
  (:require [example.five :as five]))

(defn use-ex-six-fully-qualified []
  (example.with_dash.six.SomeType. :type)
  (example.with_dash.six.SomeRecord. :record))")

(def ex-a-4
  "(comment \"foobar comment here\")

(ns example.a.four)

(defn foo []
  (println \"nuf said\"))

(deftype FourType [field])

(deftype FooType [])")

(def ex-5
  "(ns example.five
  (:import [example.with_dash.six SomeType SomeRecord]))

(defn- use-type-record []
  (SomeType. :type)
  (SomeRecord. :record))")

(def ex-6-with-dash
  "(ns example.with-dash.six)

(deftype SomeType [field])

(defrecord SomeRecord [field])")

(def ex-edn
  "{:foo \"bar\"}")

(def ex-cljc
  "(ns example.cross
  #?@(:clj
  [(:require [example.seven :as seven-clj])]
  :cljs
  [(:require [example.seven :as seven-cljs])]))")

(def ex-cljc-expected
  "(ns example.cross
  #?@(:clj
  [(:require [example.clj.seven :as seven-clj])]
  :cljs
  [(:require [example.cljs.seven :as seven-cljs])]))")

(def ex-seven-clj "(ns example.seven)")

(def ex-seven-cljs "(ns example.seven)")

(def medley-user-example
  "(ns example.user.medley
 (:require [medley.core :as medley]))")

(def medley-stub "(ns medley.core)")

(def medley-user-expected
  "(ns example.user.medley
 (:require [moved.medley.core :as medley]))")

(def example-eight
  "(ns example.eight)
 (deftype EightType [])
 (deftype TypeEight [])")

(def example-nine
  "(ns example.nine
 (:import [example.eight EightType]
           example.eight.TypeEight)
 (:require [example.eight :as eight]))")

(def example-nine-expected
  "(ns example.nine
 (:import [with_dash.example.eight EightType]
           with_dash.example.eight.TypeEight)
 (:require [with-dash.example.eight :as eight]))")

(def ex-cljc2
  "(ns example.cross2)

(defn foo [] nil)")

(def ex-cljc2-expected
  "(ns example.moved.cross2)

(defn foo [] nil)")

(def ex-ten-clj
  "(ns example.ten
  (:require [example.cross2 :as cross2]))

(example.cross2/foo)

(cross2/foo)")

(def ex-ten-cljs ex-ten-clj)

(def ex-ten-clj-expected
  "(ns example.ten
  (:require [example.moved.cross2 :as cross2]))

(example.moved.cross2/foo)

(cross2/foo)")

(def ex-ten-cljs-expected ex-ten-clj-expected)

(def ex-eleven
  "(ns example.eleven)

(defn foo [] nil)")

(def ex-eleven-expected
  "(ns example.moved.eleven)

(defn foo [] nil)")

(def ex-eleven-bar
  "(ns example.eleven.bar
  (:require [example.eleven]))

(example.eleven/foo)")

(def ex-eleven-bar-expected
  "(ns example.eleven.bar
  (:require [example.moved.eleven]))

(example.moved.eleven/foo)")

(def ex-twelve
  "(ns example.twelve)
  (:require [example.twelve.bar]))

(example.twelve.bar/foo)")

(def ex-twelve-expected
  "(ns example.twelve)
  (:require [example.bar]))

(example.bar/foo)")

(def ex-twelve-bar
  "(ns example.twelve.bar)

(defn foo [] nil)")

(def ex-twelve-bar-expected
  "(ns example.bar)

(defn foo [] nil)")

(defn- create-temp-dir! [dir-name]
  (let [temp-file (File/createTempFile dir-name nil)]
    (.delete temp-file)
    (.mkdirs temp-file)
    temp-file))

(defn- create-source-file! [^File file ^String content]
  (.delete file)
  (.mkdirs (.getParentFile file))
  (.createNewFile file)
  (spit file content)
  file)

;; this test is a slightly rewritten version of the original test for c.t.namespace.move from
;; https://github.com/clojure/tools.namespace/blob/master/src/test/clojure/clojure/tools/namespace/move_test.clj
(t/deftest move-ns-test
  (let [temp-dir      (create-temp-dir! "tools-namespace-t-move-ns")
        src-dir       (io/file temp-dir "src")
        example-dir   (io/file temp-dir "src" "example")
        a-dir         (io/file temp-dir "src" "example" "a")
        with-dash-dir (io/file temp-dir "src" "example" "with_dash")
        file-one      (create-source-file! (io/file example-dir "one.clj") ex-1)
        file-two      (create-source-file! (io/file example-dir "two.clj") ex-2)
        file-three    (create-source-file! (io/file example-dir "three.clj") ex-3)
        old-file-four (create-source-file! (io/file a-dir "four.clj") ex-a-4)
        new-file-four (io/file example-dir "b" "four.clj")
        file-five     (create-source-file! (io/file example-dir "five.clj") ex-5)
        old-file-six  (create-source-file! (io/file with-dash-dir "six.clj") ex-6-with-dash)
        new-file-six  (io/file example-dir "prefix" "with_dash" "six.clj")
        file-edn      (create-source-file! (io/file example-dir "edn.clj") ex-edn)
        file-cljc     (create-source-file! (io/file example-dir "cross.cljc") ex-cljc)
        file-seven-clj  (create-source-file! (io/file example-dir "seven.clj") ex-seven-clj)
        file-seven-cljs (create-source-file! (io/file example-dir "seven.cljs") ex-seven-cljs)
        medley-dir    (io/file src-dir "medley")
        file-medley   (create-source-file! (io/file medley-dir "core.clj") medley-stub)
        file-medley-user (create-source-file! (io/file example-dir "user" "medley.clj") medley-user-example)
        file-eight    (create-source-file! (io/file example-dir "eight.clj") example-eight)
        file-nine     (create-source-file! (io/file example-dir "nine.clj") example-nine)
        old-file-cljc2    (create-source-file! (io/file example-dir "cross2.cljc") ex-cljc2)
        new-file-cljc2    (io/file example-dir "moved" "cross2.cljc")
        file-ten-clj  (create-source-file! (io/file example-dir "ten.clj") ex-ten-clj)
        file-ten-cljs (create-source-file! (io/file example-dir "ten.cljs") ex-ten-cljs)
        old-file-eleven (create-source-file! (io/file example-dir "eleven.clj") ex-eleven)
        new-file-eleven (io/file example-dir "moved" "eleven.clj")
        file-eleven-bar (create-source-file! (io/file example-dir "eleven" "bar.clj") ex-eleven-bar)
        old-file-twelve-bar (create-source-file! (io/file example-dir "twelve" "bar.clj") ex-twelve-bar)
        new-file-twelve-bar (io/file example-dir "bar.clj")
        file-twelve   (create-source-file! (io/file example-dir "twelve.clj") ex-twelve)]

    (let [file-three-last-modified (.lastModified file-three)]

      (Thread/sleep 1500) ;; ensure file timestamps are different
      (t/testing "move ns simple case, no dash, no deftype, defrecord"
        (sut/move-ns 'example.a.four 'example.b.four src-dir ".clj" [src-dir])

        ;; (println "affected after move")
        ;; (doseq [a [file-one file-two new-file-four]]
        ;;   (println (.getAbsolutePath a))
        ;;   (prn (slurp a)))
        ;; (println "unaffected after move")
        ;; (doseq [a [file-three file-edn]]
        ;;   (println (.getAbsolutePath a))
        ;;   (prn (slurp a)))

        (t/is (.exists new-file-four)
              "new file should exist")
        (t/is (not (.exists old-file-four))
              "old file should not exist")
        (t/is (not (.exists (.getParentFile old-file-four)))
              "old empty directory should not exist")
        (t/is (= file-three-last-modified (.lastModified file-three))
              "unaffected file should not have been modified")
        (t/is (not-any? #(.contains (slurp %) "example.a.four")
                        [file-one file-two file-three new-file-four])
              "affected files should not refer to old ns")
        (t/is (.contains (slurp file-one) "(example.b.four/foo)")
              "file with a reference to ns in body should refer with a symbol")
        (t/is (every? #(.contains (slurp %) "example.b.four")
                      [file-one file-two new-file-four])
              "affected files should refer to new ns")
        (t/is (= 9 (count (re-seq #"example.b.four" (slurp file-two))))
              "all occurances of old ns should be replace with new")
        (t/is (re-find #"\(:example.b.four/" (slurp file-one))
              "type of occurence is retained if keyword")
        (t/is (re-find #"\[example\.b\s*\[foo\]\s*\[bar\]\]" (slurp file-two))
              "prefixes should be replaced")
        (t/is (= ex-edn (slurp file-edn))
              "clj file wo/ ns macro is unchanged"))

      (t/testing "testing import deftype no dash, dash in the prefix"
        (sut/move-ns 'example.eight 'with-dash.example.eight src-dir ".clj" [src-dir])

        (t/is (= (slurp file-nine) example-nine-expected)))

      (t/testing "move ns with dash, deftype, defrecord, import"
        (sut/move-ns 'example.with-dash.six 'example.prefix.with-dash.six src-dir ".clj" [src-dir])

        ;; (println "affected after move")
        ;; (doseq [a [file-three file-five new-file-six new-file-four]]
        ;;   (println (.getAbsolutePath a))
        ;;   (prn (slurp a)))

        (t/is (.exists new-file-six)
              "new file should exist")
        (t/is (not (.exists old-file-six))
              "old file should not exist")
        (t/is (not-any? #(.contains (slurp %) "example.with_dash.six")
                        [file-five file-three])
              "affected files should not refer to old ns in imports or body")
        (t/is (every? #(.contains (slurp %) "example.prefix.with_dash.six")
                      [file-five file-three])
              "affected files should refer to new ns"))

      (t/testing "testing cljc file using :clj/cljs macros in require depending on same ns in clj and cljs"
        (sut/move-ns 'example.seven 'example.clj.seven src-dir ".clj" [src-dir])
        (sut/move-ns 'example.seven 'example.cljs.seven src-dir ".cljs" [src-dir])

        (t/is (= (slurp file-cljc) ex-cljc-expected)))

      (t/testing "testing alias is first section of two section namespace"
        (sut/move-ns 'medley.core 'moved.medley.core src-dir ".clj" [src-dir])

        (t/is (= (slurp file-medley-user) medley-user-expected)))


      (t/testing "testing moving cljc and corresponding clj/cljc spaces"
        (sut/move-ns 'example.cross2 'example.moved.cross2 src-dir ".cljc" [src-dir])

        (t/is (.exists new-file-cljc2)
              "new file should exist")
        (t/is (= (slurp new-file-cljc2) ex-cljc2-expected)
              "moved cljc file should have new location")
        (t/is (= (slurp file-ten-clj) ex-ten-clj-expected)
              "affected clj file not correct")
        (t/is (= (slurp file-ten-cljs) ex-ten-cljs-expected)
              "affected cljs file not correct"))

      (t/testing "testing moving namespace which is prefix of another ns"
        (sut/move-ns 'example.eleven 'example.moved.eleven src-dir ".clj" [src-dir])

        (t/is (.exists new-file-eleven)
              "new file should exist")
        (t/is (= (slurp new-file-eleven) ex-eleven-expected)
              "moved clj file should have new location")
        (t/is (= (slurp file-eleven-bar) ex-eleven-bar-expected)
              "old prefix namespace should only change with respect to prefix ns"))

      (t/testing "testing moving namespace where prefix is another ns"
        (sut/move-ns 'example.twelve.bar 'example.bar src-dir ".clj" [src-dir])

        (t/is (.exists new-file-twelve-bar)
              "new file should exist")
        (t/is (= (slurp new-file-twelve-bar) ex-twelve-bar-expected)
              "moved clj file should have new location")
        (t/is (= (slurp file-twelve) ex-twelve-expected)
              "prefixed namespace should only change with respect to moved ns")))))
