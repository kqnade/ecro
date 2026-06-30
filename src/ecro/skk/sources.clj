(ns ecro.skk.sources
  "Combine multiple SKK dictionary sources into a single lookup function.

  Sources include personal dictionary files, optional large dictionaries, and
  optional SKK server (yaskkserv2)."
  (:require
    [ecro.skk.config :as skk-config]
    [ecro.skk.jisyo :as jisyo]
    [ecro.skk.skk-server :as skk-server]))


(defonce ^:private server-available-state
  (atom nil))


(defn load-file-dict
  "Load dictionary from configured file paths.

  Returns a parsed dict map with :okuri-ari and :okuri-nasi. Missing files are
  ignored."
  ([]
   (load-file-dict (skk-config/load-jisyo-paths)))
  ([paths]
   (let [jisyo (when (:jisyo-path paths) (jisyo/parse (slurp (:jisyo-path paths))))
         large (when (:large-jisyo-path paths) (jisyo/parse (slurp (:large-jisyo-path paths))))]
     {:okuri-ari (merge (:okuri-ari large {}) (:okuri-ari jisyo {}))
      :okuri-nasi (merge (:okuri-nasi large {}) (:okuri-nasi jisyo {}))})))


(defn- file-candidates
  "Return candidates from a parsed dict map."
  [dict midashi okuri-char]
  (if okuri-char
    (jisyo/candidates dict midashi okuri-char)
    (jisyo/candidates dict midashi)))


(defn- server-candidates
  "Query the SKK server once and cache availability.

  Returns nil when the server is unavailable so that make-lookup can fall
  through to an empty result."
  [server-cfg midashi okuri-char]
  (when (not= false @server-available-state)
    (let [cands (skk-server/candidates midashi okuri-char server-cfg)]
      (if (seq cands)
        (do (reset! server-available-state true)
            cands)
        (do (when (nil? @server-available-state)
              (reset! server-available-state false))
            nil)))))


(defn make-lookup
  "Create a lookup function that searches sources in order.

  Sources map may contain:
  - :dict      parsed jisyo map
  - :skk-server config map for SKK server (yaskkserv2)

  The returned function takes [midashi okuri-char] and returns a vector of
  candidates."
  [sources]
  (let [dict (:dict sources)
        server-cfg (:skk-server sources)]
    (fn [midashi okuri-char]
      (or (seq (file-candidates dict midashi okuri-char))
          (when server-cfg
            (seq (server-candidates server-cfg midashi okuri-char)))
          []))))


(defn default-sources
  "Build default sources from ecro config and environment.

  Always includes the SKK server configuration for 127.0.0.1:1178; availability
  is determined lazily during lookup."
  ([]
   (default-sources (skk-config/load-jisyo-paths)))
  ([paths]
   {:dict (load-file-dict paths)
    :skk-server {:host "127.0.0.1" :port 1178 :encoding "EUC-JP"}}))


(defonce default-lookup-fn
  (delay (make-lookup (default-sources))))


(defn default-lookup
  "Create a default lookup function using environment discovery.

  The result is cached after the first call."
  []
  @default-lookup-fn)
