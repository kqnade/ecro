(ns ecro.skk.config
  (:require
    [clojure.java.io :as io]
    [ecro.config :as config]))


(defn- default-jisyo-path
  "Default personal dictionary path: ~/.skk-jisyo"
  []
  (.getPath (io/file (System/getProperty "user.home") ".skk-jisyo")))


(defn- home-file
  "Return a file under the user's home directory."
  [name]
  (io/file (System/getProperty "user.home") name))


(defn- read-skk-setq-from-file
  "Read skk-jisyo and skk-large-jisyo setq forms from a single Elisp file."
  [file]
  (let [text (slurp file)]
    (into {}
          (keep (fn [[_ key value]]
                  (when (#{"skk-jisyo" "skk-large-jisyo"} key)
                    [(keyword key) value])))
          (re-seq #"\(setq\s+(skk-jisyo|skk-large-jisyo)\s+\"([^\"]+)\"" text))))


(defn- read-skk-init-setq
  "Conservatively read (setq skk-jisyo \"...\") and (setq skk-large-jisyo \"...\")
  from ~/.skk configuration without evaluating any Elisp.

  If ~/.skk is a file, read it directly. If it is a directory, search
  *.el files inside it and merge the discovered values."
  []
  (let [file (home-file ".skk")]
    (cond
      (and (.exists file) (.isFile file))
      (read-skk-setq-from-file file)

      (and (.exists file) (.isDirectory file))
      (apply merge
             (map read-skk-setq-from-file
                  (filter #(clojure.string/ends-with? (.getName %) ".el")
                          (.listFiles file))))

      :else
      {})))


(defn- expand-path
  "Expand ~/ prefix in a path string. Returns nil for nil input."
  [path]
  (when path
    (if (clojure.string/starts-with? path "~/")
      (.getPath (io/file (System/getProperty "user.home") (subs path 2)))
      path)))


(defn- resolve-path
  "Return expanded path if file exists, otherwise nil."
  [path]
  (when-let [expanded (expand-path path)]
    (let [file (io/file expanded)]
      (when (.exists file)
        (.getPath file)))))


(defn- first-existing
  "Return the first path in the list that exists on disk."
  [paths]
  (first (keep resolve-path paths)))


(defn load-jisyo-paths
  "Discover SKK dictionary paths.

  Resolution order:
  1. ecro config `:skk {:jisyo-path ... :large-jisyo-path ...}`
  2. Conservative extraction from ~/.skk
  3. ~/.skk-jisyo for personal dictionary

  Returns {:jisyo-path ... :large-jisyo-path ...} where missing paths are nil."
  ([]
   (load-jisyo-paths (config/load-config (io/file (System/getProperty "user.home")
                                                  ".ecro"
                                                  "init.edn"))))
  ([ecro-config]
   (let [ecro-skk (get-in ecro-config [:skk] {})
         init-config (read-skk-init-setq)
         jisyo (first-existing [(get ecro-skk :jisyo-path)
                                (get init-config :skk-jisyo)
                                (default-jisyo-path)])
         large (first-existing [(get ecro-skk :large-jisyo-path)
                                (get init-config :skk-large-jisyo)])]
     {:jisyo-path jisyo
      :large-jisyo-path large})))
