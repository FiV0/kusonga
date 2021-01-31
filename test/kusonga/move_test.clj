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
  (:require [example2.bar]))

(example2.bar/foo)")

(def ex-twelve-bar
  "(ns example.twelve.bar)

(defn foo [] nil)")

(def ex-twelve-bar-expected
  "(ns example2.bar)

(defn foo [] nil)")

(def ex-13
  "(ns example.thirteen.bar)")

(def ex-edn-prefixed-key
  "{:example.thirteen.foo/bar 1
    example.thirteen.foo 2
    :example.thirteen.toto/bar 3}")


(def ex-edn-prefixed-key-expected
  "{:example.thirteen.bar/bar 1
    example.thirteen.bar 2
    :example.thirteen.toto/bar 3}")

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
  (let [temp-dir             (create-temp-dir! "tools-namespace-t-move-ns")
        src-dir              (io/file temp-dir "src")
        example-dir          (io/file temp-dir "src" "example")
        a-dir                (io/file temp-dir "src" "example" "a")
        with-dash-dir        (io/file temp-dir "src" "example" "with_dash")
        file-one             (create-source-file! (io/file example-dir "one.clj") ex-1)
        file-two             (create-source-file! (io/file example-dir "two.clj") ex-2)
        file-three           (create-source-file! (io/file example-dir "three.clj") ex-3)
        old-file-four        (create-source-file! (io/file a-dir "four.clj") ex-a-4)
        new-file-four        (io/file example-dir "b" "four.clj")
        file-five            (create-source-file! (io/file example-dir "five.clj") ex-5)
        old-file-six         (create-source-file! (io/file with-dash-dir "six.clj") ex-6-with-dash)
        new-file-six         (io/file example-dir "prefix" "with_dash" "six.clj")
        file-edn             (create-source-file! (io/file example-dir "edn.clj") ex-edn)
        file-cljc            (create-source-file! (io/file example-dir "cross.cljc") ex-cljc)
        _file-seven-clj      (create-source-file! (io/file example-dir "seven.clj") ex-seven-clj)
        _file-seven-cljs     (create-source-file! (io/file example-dir "seven.cljs") ex-seven-cljs)
        new-file-seven-clj   (io/file example-dir "moved" "seven.clj")
        new-file-seven-cljs  (io/file example-dir "moved" "seven.cljs")
        medley-dir           (io/file src-dir "medley")
        _file-medley         (create-source-file! (io/file medley-dir "core.clj") medley-stub)
        file-medley-user     (create-source-file! (io/file example-dir "user" "medley.clj") medley-user-example)
        _file-eight          (create-source-file! (io/file example-dir "eight.clj") example-eight)
        file-nine            (create-source-file! (io/file example-dir "nine.clj") example-nine)
        _old-file-cljc2      (create-source-file! (io/file example-dir "cross2.cljc") ex-cljc2)
        new-file-cljc2       (io/file example-dir "moved" "cross2.cljc")
        file-ten-clj         (create-source-file! (io/file example-dir "ten.clj") ex-ten-clj)
        file-ten-cljs        (create-source-file! (io/file example-dir "ten.cljs") ex-ten-cljs)
        _old-file-eleven     (create-source-file! (io/file example-dir "eleven.clj") ex-eleven)
        new-file-eleven      (io/file example-dir "moved" "eleven.clj")
        file-eleven-bar      (create-source-file! (io/file example-dir "eleven" "bar.clj") ex-eleven-bar)
        _old-file-twelve-bar (create-source-file! (io/file example-dir "twelve" "bar.clj") ex-twelve-bar)
        new-file-twelve-bar  (io/file src-dir "example2" "bar.clj")
        file-twelve          (create-source-file! (io/file example-dir "twelve.clj") ex-twelve)
        _file-thirteen       (create-source-file! (io/file example-dir "thirteen" "foo.clj") ex-13)
        file-edn-prefixed    (create-source-file! (io/file src-dir "edn-prefixed.edn") ex-edn-prefixed-key)]

    (let [file-three-last-modified (.lastModified file-three)]

      (Thread/sleep 1500) ;; ensure file timestamps are different
      (t/testing "move ns simple case, no dash, no deftype, defrecord"
        (sut/move-ns 'example.a.four 'example.b.four src-dir [src-dir])

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
        (sut/move-ns 'example.eight 'with-dash.example.eight src-dir [src-dir])

        (t/is (= (slurp file-nine) example-nine-expected)))

      (t/testing "move ns with dash, deftype, defrecord, import"
        (sut/move-ns 'example.with-dash.six 'example.prefix.with-dash.six src-dir [src-dir])

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

      (t/testing (str "testing cljc file using :clj/cljs macros in"
                      "require depending on same ns in clj and cljs in one go")
        (create-source-file! (io/file example-dir "seven.clj") ex-seven-clj)
        (create-source-file! (io/file example-dir "seven.cljs") ex-seven-cljs)
        (sut/move-ns 'example.seven 'example.moved.seven src-dir [src-dir])

        (t/is (= (slurp file-cljc) ex-cljc-expected))

        (t/is (.exists new-file-seven-clj)
              "new clj file should exist")
        (t/is (.exists new-file-seven-cljs)
              "new cljs file should exist"))

      (t/testing "testing alias is first section of two section namespace"
        (sut/move-ns 'medley.core 'moved.medley.core src-dir [src-dir])

        (t/is (= (slurp file-medley-user) medley-user-expected)))


      (t/testing "testing moving cljc and corresponding clj/cljc spaces"
        (sut/move-ns 'example.cross2 'example.moved.cross2 src-dir [src-dir])

        (t/is (.exists new-file-cljc2)
              "new file should exist")
        (t/is (= (slurp new-file-cljc2) ex-cljc2-expected)
              "moved cljc file should have new location")
        (t/is (= (slurp file-ten-clj) ex-ten-clj-expected)
              "affected clj file not correct")
        (t/is (= (slurp file-ten-cljs) ex-ten-cljs-expected)
              "affected cljs file not correct"))

      (t/testing "testing moving namespace which is prefix of another ns"
        (sut/move-ns 'example.eleven 'example.moved.eleven src-dir [src-dir])

        (t/is (.exists new-file-eleven)
              "new file should exist")
        (t/is (= (slurp new-file-eleven) ex-eleven-expected)
              "moved clj file should have new location")
        (t/is (= (slurp file-eleven-bar) ex-eleven-bar-expected)
              "old prefix namespace should only change with respect to prefix ns"))

      (t/testing "testing moving namespace where prefix is another ns"
        (sut/move-ns 'example.twelve.bar 'example2.bar src-dir [src-dir])

        (t/is (.exists new-file-twelve-bar)
              "new file should exist")
        (t/is (= (slurp new-file-twelve-bar) ex-twelve-bar-expected)
              "moved clj file should have new location")
        (t/is (= (slurp file-twelve) ex-twelve-expected)
              "prefixed namespace should only change with respect to moved ns"))

      (t/testing "prefixed edn is changed"
        (sut/move-ns 'example.thirteen.foo 'example.thirteen.bar src-dir [src-dir])

        (t/is (= (slurp file-edn-prefixed) ex-edn-prefixed-key-expected)
              "prefixed namespace should change in edn files")))))

(def ex-not "(ns not.example)")

(def ex-1-expected-prefix
  "(ns hello.one
  (:require [hello.two :as two]
            [hello.three :as three]))

(defn foo [m]
  (:hello.a.four/bar m)
  (hello.a.four/foo))")

(def ex-2-expected-prefix
  "(ns
  hello.two
  (:require [hello.three :as three]
            [hello.a.four :as four]
            [hello.a
             [foo]
             [bar]])
  (:import [hello.a.four FourType]
           hello.a.four.FooType))

(defn foo []
  (hello.a.four/foo))

(defn cljs-foo
  \"This is valid in cljs i am told.\"
  []
  (hello.a.four.foo))

(def delayed-four
  (do
    (require 'hello.a.four)
    (resolve 'hello.a.four/foo)))

(defn my-four-type
  ^hello.a.four.FourType
  [^hello.a.four.FourType t]
  t)")

(def ex-3-expected-prefix
  "(ns hello.three
  (:require [hello.five :as five]))

(defn use-ex-six-fully-qualified []
  (hello.with_dash.six.SomeType. :type)
  (hello.with_dash.six.SomeRecord. :record))")

(def ex-a-4-expected-prefix
  "(comment \"foobar comment here\")

(ns hello.a.four)

(defn foo []
  (println \"nuf said\"))

(deftype FourType [field])

(deftype FooType [])")

(def ex-5-expected-prefix
  "(ns hello.five
  (:import [hello.with_dash.six SomeType SomeRecord]))

(defn- use-type-record []
  (SomeType. :type)
  (SomeRecord. :record))")

(def ex-6-with-dash-expected-prefix
  "(ns hello.with-dash.six)

(deftype SomeType [field])

(defrecord SomeRecord [field])")

(def ex-14-cljs "(ns example.fourteen)")

(def ex-14-cljs-expected "(ns hello.fourteen)")

(def ex-14-clj-with-cljs-ref
  "(ns hello.a.foo)

  {'example.fourteen.bar 1}")

(def ex-14-clj-with-cljs-ref-expected
  "(ns hello.a.foo)

  {'hello.fourteen.bar 1}")

(def ex-15-prefix
  "(ns example.fifeteen)

  {'example.fifeteen.bar 1}")

(def ex-15-expected-prefix
  "(ns hello.fifeteen)

  {'hello.fifeteen.bar 1}")

(def ex-16-prefix
  "(ns hello.sixteen)

  (defrecord HelloSixteen [])")

(def ex-16-prefix-expected
  "(ns hello.bar.sixteen)

  (defrecord HelloSixteen [])")

(def ex-16-imported
  "(ns hello.sixteen.foo
     (:import (hello.sixteen HelloSixteen)))")

(def ex-16-imported-expected
  "(ns hello.bar.sixteen.foo
     (:import (hello.bar.sixteen HelloSixteen)))")

(t/deftest rename-prefix-test
  (let [temp-dir                (create-temp-dir! "tools-namespace-t-move-ns")
        src-dir                 (io/file temp-dir "src")
        example-dir             (io/file temp-dir "src" "example")
        hello-dir               (io/file temp-dir "src" "hello")
        a-dir                   (io/file temp-dir "src" "example" "a")
        hello-a-dir             (io/file hello-dir "a")
        with-dash-dir           (io/file temp-dir "src" "example" "with_dash")
        hello-with-dash-dir     (io/file hello-dir "with_dash")
        not-dir                 (io/file temp-dir "src" "not")
        hello-bar-dir           (io/file hello-dir "bar")
        hello-16-dir            (io/file hello-dir "sixteen")
        hello-bar-16-dir        (io/file hello-bar-dir "sixteen")
        _file-one               (create-source-file! (io/file example-dir "one.clj") ex-1)
        file-one-moved          (io/file hello-dir "one.clj")
        _file-two               (create-source-file! (io/file example-dir "two.clj") ex-2)
        file-two-moved          (io/file hello-dir "two.clj")
        _file-three             (create-source-file! (io/file example-dir "three.clj") ex-3)
        file-three-moved        (io/file hello-dir "three.clj")
        _file-four              (create-source-file! (io/file a-dir "four.clj") ex-a-4)
        file-four-moved         (io/file hello-a-dir "four.clj")
        _file-five              (create-source-file! (io/file example-dir "five.clj") ex-5)
        file-five-moved         (io/file hello-dir "five.clj")
        _file-six               (create-source-file! (io/file with-dash-dir "six.clj") ex-6-with-dash)
        file-six-moved          (io/file hello-with-dash-dir "six.clj")
        file-not-example        (create-source-file! (io/file not-dir "example.clj") ex-not)
        _file-fourteen          (create-source-file! (io/file example-dir "fourteen.cljs") ex-14-cljs)
        file-fourteen-moved     (io/file hello-dir "fourteen.cljs")
        file-fourteen-clj       (create-source-file! (io/file hello-a-dir "foo.clj") ex-14-clj-with-cljs-ref)
        _file-fifeteen          (create-source-file! (io/file example-dir "fifeteen.clj") ex-15-prefix)
        file-fifeteen-moved     (io/file hello-dir "fifeteen.clj")
        _file-sixteen           (create-source-file! (io/file hello-dir "sixteen.clj") ex-16-prefix)
        file-sixteen-moved      (io/file hello-bar-dir "sixteen.clj")
        _file-bar-sixteen       (create-source-file! (io/file hello-16-dir "foo.clj") ex-16-imported)
        file-bar-sixteen-moved  (io/file hello-bar-16-dir "foo.clj")]

    (Thread/sleep 1500) ;; ensure file timestamps are different
    (t/testing "testing replacing prefix of namespaces"
      (sut/rename-prefix 'example 'hello [src-dir])

      (t/is (= (slurp file-one-moved) ex-1-expected-prefix)
            "moved file 1 not correct")
      (t/is (= (slurp file-two-moved) ex-2-expected-prefix)
            "moved file 2 not correct")
      (t/is (= (slurp file-three-moved) ex-3-expected-prefix)
            "moved file 3 not correct")
      (t/is (= (slurp file-four-moved) ex-a-4-expected-prefix)
            "moved file 4 not correct")
      (t/is (= (slurp file-five-moved) ex-5-expected-prefix)
            "moved file 5 not correct")
      (t/is (= (slurp file-six-moved) ex-6-with-dash-expected-prefix)
            "moved file 6 not correct")
      (t/is (= (slurp file-not-example) ex-not)
            "non prefixed ns should not be changed")
      (t/is (= (slurp file-fourteen-moved) ex-14-cljs-expected)
            "moved cljs file not correct")
      (t/is (= (slurp file-fourteen-clj) ex-14-clj-with-cljs-ref-expected)
            "cljs ref in clj file not updated")
      (t/is (= (slurp file-fifeteen-moved) ex-15-expected-prefix)
            "moved clj file with symbol"))

    (t/testing "testing renaming prefix with exact namespace match"
      (sut/rename-prefix 'hello.sixteen 'hello.bar.sixteen [src-dir])

      (t/is (= (slurp file-sixteen-moved) ex-16-prefix-expected)
            "exact namespace moved not working correctly")
      (t/is (= (slurp file-bar-sixteen-moved) ex-16-imported-expected)
            "prefix namespace moved not working correctly"))))

(comment
  (rename-prefix-test))
