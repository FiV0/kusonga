# kusonga

[![Clojars Project](https://img.shields.io/clojars/v/fiv0/kusonga)](https://clojars.org/fiv0/kusonga)
[![cljdoc badge](https://cljdoc.org/badge/fiv0/kusonga?0.1.1)](https://cljdoc.org/d/fiv0/kusonga/CURRENT)

Renaming and moving namespaces.

----

The library is mainly an extraction from
[mranderson](https://github.com/benedekfazekas/mranderson)'s move namespace. It allows you
to rename namespaces as well as your whole project across `clj`, `cljc` and `cljs` files.
As clojure is a dynamic language this won't work in all cases, but should work for anything
reasonable.

```clj
(require '[kusonga.move :as move])

(move/move-ns 'old.namespace.name
              'new.namespace.name
              src-dir ; where to find the namespace
              [src-dir another-dir]) ; directories where to rename occurences

;; in case one wants to only move the Clojure namespace
(move/move-ns 'old.namespace.name 'new-namespace-name src-dir ".clj" [src-dir another-dir])
```

In case you want to rename your project there is a function to replace the prefix of all
namespaces that match.

```clj
(move/rename-prefix 'io.my-cool-app 'com.my-awesome-app dir)
```

Beware that moving and renaming namespaces also affects `edn` files.
Namespaced keys and symbols are affected whenever the
corresponding namespace gets moved.
```edn
{:foo.bar/toto nil
 foo.bar nil
 :foo.toto/bar nil}
```
becomes

```edn
{:foo.fizz/toto nil
 foo.fizz nil
 :foo.toto/bar nil}
```
when renaming the namespace `'foo.bar` to `'foo.fizz`.

## Test
Tests can be run via
```bash
clojure -X:test
```

## Contributing

If you find a case where the library does do what it is expected to do,
please file an issue. PR's are welcome.

## Previous work
- [clojure.tools.namespace](https://github.com/clojure/tools.namespace)
- [mranderson](https://github.com/benedekfazekas/mranderson)

## License

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
