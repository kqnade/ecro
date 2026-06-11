(ns build
  (:require
    [clojure.tools.build.api :as b]))


(def class-dir "target/classes")


(defn aot
  "AOT compile ecro namespaces for GraalVM native-image."
  [_]
  (b/delete {:path class-dir})
  (let [basis (b/create-basis {:project "deps.edn"})]
    (b/compile-clj {:basis basis
                    :src-dirs ["src"]
                    :class-dir class-dir
                    :ns-compile '[ecro.main]})))
