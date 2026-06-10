(ns ecro.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

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

(defn merge-config
  "Merge config into editor state."
  [state config]
  (merge state config))

(defn default-config
  "Return default configuration."
  []
  {:theme :light
   :keymap {}})
