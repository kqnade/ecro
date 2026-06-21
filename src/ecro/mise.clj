(ns ecro.mise
  (:require
    [clojure.java.io :as io]))


(defn- git-root
  "Return the git root directory starting from dir, or nil."
  [dir]
  (loop [current dir]
    (when current
      (let [git-dir (io/file current ".git")]
        (if (.exists git-dir)
          (.getAbsolutePath current)
          (let [parent (.getParentFile current)]
            (when (and parent (.isDirectory parent))
              (recur parent))))))))


(defn find-mise-toml
  "Search for mise.toml from the directory of filepath up to home-dir or git root.
   Returns the absolute path string of the first found mise.toml, or nil."
  [filepath home-dir]
  (let [start-dir (.getParentFile (io/file filepath))
        home-path (when home-dir (.getAbsolutePath (io/file home-dir)))
        git-root-path (git-root start-dir)
        stop-paths (cond-> #{}
                     home-path (conj home-path)
                     git-root-path (conj git-root-path))]
    (loop [dir start-dir]
      (when dir
        (let [mise (io/file dir "mise.toml")
              dir-path (.getAbsolutePath dir)]
          (cond
            (.exists mise) (.getAbsolutePath mise)
            (contains? stop-paths dir-path) nil
            :else (recur (.getParentFile dir))))))))
