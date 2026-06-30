(ns ecro.skk.skk-server
  "SKK dictionary server client.

  Implements the SKK server protocol used by yaskkserv2 and similar servers.
  Queries are sent as plain TCP requests and responses are decoded in EUC-JP
  by default."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))


(def ^:private default-config
  {:host "127.0.0.1"
   :port 1178
   :encoding "EUC-JP"
   :timeout-ms 1000})


(defn- encode
  "Encode a string using the configured encoding."
  [s encoding]
  (.getBytes ^String s ^String encoding))


(defn- decode
  "Decode bytes using the configured encoding."
  [bytes encoding]
  (String. ^bytes bytes ^String encoding))


(defn- send-request
  "Send a request to the SKK server and return the response bytes."
  [request-bytes config]
  (try
    (with-open [socket (java.net.Socket.)
                _ (.connect socket
                            (java.net.InetSocketAddress. (:host config) (:port config))
                            (:timeout-ms config))
                out (.getOutputStream socket)
                in (.getInputStream socket)]
      (.write out request-bytes)
      (.flush out)
      (.shutdownOutput socket)
      (let [baos (java.io.ByteArrayOutputStream.)]
        (io/copy in baos)
        (.toByteArray baos)))
    (catch Exception _
      nil)))


(defn build-request
  "Build SKK server request bytes.

  Okuri-nasi: 1<midashi><space>
  Okuri-ari:  2<midashi><space><okuri-char><space>"
  [midashi okuri-char encoding]
  (let [req (if okuri-char
              (str "2" midashi " " okuri-char " ")
              (str "1" midashi " "))]
    (encode req encoding)))


(defn parse-response
  "Parse SKK server response bytes into a candidate vector.

  Successful response: 1/cand1/cand2/...
  Not found response:  4<midashi><space>
  Annotation format:   cand;annotation"
  [response-bytes encoding]
  (when (seq response-bytes)
    (let [response (decode response-bytes encoding)
          trimmed (str/trim response)]
      (when (str/starts-with? trimmed "1")
        (let [candidates-str (subs trimmed 1)]
          (->> (str/split candidates-str #"/")
               (remove str/blank?)
               (mapv #(first (str/split % #";")))))))))


(defn candidates
  "Query the SKK server for candidates.

  Accepts an optional config map with :host, :port, :encoding, :timeout-ms.
  Returns a vector of candidate strings, or empty vector on failure."
  ([midashi]
   (candidates midashi nil {}))
  ([midashi okuri-char]
   (candidates midashi okuri-char {}))
  ([midashi okuri-char config]
   (let [cfg (merge default-config config)
         req (build-request midashi okuri-char (:encoding cfg))
         resp (send-request req cfg)]
     (or (parse-response resp (:encoding cfg)) []))))


(defn available?
  "Return true if the configured SKK server responds."
  ([]
   (available? {}))
  ([config]
   (let [cfg (merge default-config config)]
     (boolean (send-request (encode "1a " (:encoding cfg)) cfg)))))
