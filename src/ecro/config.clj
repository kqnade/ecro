(ns ecro.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [sci.core :as sci]))


(defn load-config
  "Load configuration from an EDN file. Returns empty map if file doesn't exist."
  [filepath]
  (let [file (io/file filepath)]
    (if (.exists file)
      (try
        (edn/read-string (slurp file))
        (catch Exception e
          (println "Warning: Could not parse config file:" (.getMessage e))
          {}))
      {})))


(defn load-sci-config
  "Load and evaluate init.clj via SCI with given bindings. Returns the SCI context result map."
  [filepath bindings]
  (let [file (io/file filepath)]
    (if (.exists file)
      (try
        (let [ctx (sci/init {:bindings bindings})]
          (sci/eval-string* ctx (slurp file))
          {:loaded true})
        (catch Exception e
          (println "Warning: Could not evaluate init.clj:" (.getMessage e))
          {}))
      {})))


(defn merge-config
  "Merge config into editor state."
  [state config]
  (merge state config))


(defn default-config
  "Return default configuration."
  []
  {:theme :light
   :keymap {}})
