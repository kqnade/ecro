(ns ecro.skk.sources
  "Combine multiple SKK dictionary sources into a single lookup function.

  Sources include personal dictionary files, optional large dictionaries, and
  optional SKK server (yaskkserv2)."
  (:require
    [ecro.skk.config :as skk-config]
    [ecro.skk.jisyo :as jisyo]
    [ecro.skk.skk-server :as skk-server]))


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
          (when (and server-cfg (skk-server/available? server-cfg))
            (seq (skk-server/candidates midashi okuri-char server-cfg)))
          []))))


(defn default-sources
  "Build default sources from ecro config and environment.

  Tries to connect to yaskkserv2 on 127.0.0.1:1178 if no explicit config."
  ([]
   (default-sources (skk-config/load-jisyo-paths)))
  ([paths]
   (let [server-cfg {:host "127.0.0.1" :port 1178 :encoding "EUC-JP"}]
     {:dict (load-file-dict paths)
      :skk-server (when (skk-server/available? server-cfg) server-cfg)})))


(defn default-lookup
  "Create a default lookup function using environment discovery."
  []
  (make-lookup (default-sources)))
