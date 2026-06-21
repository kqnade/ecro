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


(defn normalize-tool-name
  "Normalize a mise tool name into a keyword.
   Strips version suffixes and path/backend prefixes, then kebab-cases the remainder."
  [tool-name]
  (let [base (-> tool-name
                 str/lower-case
                 (str/split #"@")
                 first)
        name-only (last (str/split base #"[:/]"))
        normalized (str/replace name-only #"[^a-z0-9]+" "-")]
    (keyword normalized)))


(defn normalize-tools
  "Normalize a sequence of mise tool names into a sorted set of keywords."
  [tools]
  (->> tools
       (map normalize-tool-name)
       (into (sorted-set))))


(defn load-tools
  "Load and parse the [tools] section from a mise.toml file.
   Returns a vector of tool name strings, or nil if the file is missing."
  [mise-path]
  (when (and mise-path (.exists (io/file mise-path)))
    (parse-tools (slurp mise-path))))


(def ^:private mise-detection-cache
  "Cache of project tool detection results keyed by project root path."
  (atom {}))


(defn clear-cache!
  "Clear the global mise detection cache."
  []
  (reset! mise-detection-cache {}))


(defn cache-result
  "Cache a detection result for the given project root."
  [root result]
  (swap! mise-detection-cache assoc root result))


(defn cached-result
  "Return a cached detection result for the given project root, or nil."
  [root]
  (get @mise-detection-cache root))


(defn detect-project-tools
  "Detect project tools for a file path. Returns a map with:
   :mise-path, :tools, :analyzer-candidates, :lsp-candidates."
  [filepath home-dir]
  (if-let [mise-path (find-mise-toml filepath home-dir)]
    {:mise-path mise-path
     :tools (normalize-tools (load-tools mise-path))
     :analyzer-candidates []
     :lsp-candidates []}
    {:mise-path nil
     :tools #{}
     :analyzer-candidates []
     :lsp-candidates []}))


(defn project-root
  "Infer a project root for caching. Prefers the directory containing
   mise.toml, falling back to the git root or the file's directory."
  [filepath home-dir]
  (or (when-let [mise-path (find-mise-toml filepath home-dir)]
        (.getParent (io/file mise-path)))
      (git-root (.getParentFile (io/file filepath)))
      (.getParent (io/file filepath))))


(defn detect-project-tools-cached
  "Detect project tools, using and updating the global cache."
  [filepath home-dir]
  (let [root (project-root filepath home-dir)]
    (if-let [cached (cached-result root)]
      cached
      (let [result (detect-project-tools filepath home-dir)]
        (cache-result root result)
        result))))
