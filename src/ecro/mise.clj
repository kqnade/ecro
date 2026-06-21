(ns ecro.mise
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))


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


(defn- extract-tools-section
  "Extract the raw content of the [tools] section from TOML text."
  [content]
  (when content
    (second (re-find #"(?s)\[tools\]\s*(.*?)(?:\n\[|\z)" content))))


(defn parse-tools
  "Parse tool names from a mise.toml [tools] section.
   Returns a sequence of tool name strings."
  [content]
  (when-let [section (extract-tools-section content)]
    (->> (str/split-lines section)
         (map #(re-find #"^\s*\"?([^\"=#\s]+)\"?\s*=" %))
         (keep #(some-> % second str/trim))
         vec)))


(defn load-tools
  "Load and parse the [tools] section from a mise.toml file.
   Returns a vector of tool name strings, or nil if the file is missing."
  [mise-path]
  (when (and mise-path (.exists (io/file mise-path)))
    (parse-tools (slurp mise-path))))
