(ns ecro.skk.jisyo
  (:require
    [clojure.java.io :as io]))


(defn- trim-comment
  "Remove trailing comment from a candidate string."
  [s]
  (if-let [idx (clojure.string/index-of s ";")]
    (subs s 0 idx)
    s))


(defn- parse-candidates
  "Parse the /a/b/c/ candidate portion into a vector of candidate strings."
  [s]
  (->> (clojure.string/split s #"/")
       (remove clojure.string/blank?)
       (mapv trim-comment)))


(defn- parse-line
  "Parse a single dictionary line. Returns [midashi candidates] or nil."
  [line]
  (let [trimmed (clojure.string/trim line)]
    (when (and (seq trimmed)
               (not (clojure.string/starts-with? trimmed ";;"))
               (clojure.string/includes? trimmed " /"))
      (let [[midashi candidates-str] (clojure.string/split trimmed #" /" 2)]
        (when (and (seq midashi) (seq candidates-str))
          [midashi (parse-candidates candidates-str)])))))


(defn parse
  "Parse SKK dictionary text into {:okuri-ari {...} :okuri-nasi {...}}.

  Entries before the `;; okuri-nasi entries.` header are okuri-ari.
  Entries after are okuri-nasi. Lines starting with `;;` are ignored."
  [text]
  (let [lines (clojure.string/split-lines text)
        okuri-nasi-start (or (first (keep-indexed #(when (clojure.string/starts-with? %2 ";; okuri-nasi entries.") %1) lines))
                             (count lines))
        okuri-ari-lines (take okuri-nasi-start lines)
        okuri-nasi-lines (drop (inc okuri-nasi-start) lines)
        ->map (fn [lines]
                (into {}
                      (keep parse-line)
                      lines))]
    {:okuri-ari (->map okuri-ari-lines)
     :okuri-nasi (->map okuri-nasi-lines)}))


(defn candidates
  "Return candidates for midashi from parsed dictionary map.

  If okuri-char is provided, search okuri-ari using key `midashi + okuri-char`.
  Otherwise search okuri-nasi using midashi."
  ([dict midashi]
   (candidates dict midashi nil))
  ([dict midashi okuri-char]
   (let [key (if okuri-char (str midashi okuri-char) midashi)
         section (if okuri-char :okuri-ari :okuri-nasi)]
     (get-in dict [section key] []))))


(defn update-candidate-order
  "Move selected candidate to the front for the given midashi.

  If okuri-char is provided, the lookup key becomes `midashi + okuri-char`
  in the okuri-ari section. If the entry does not exist, it is created."
  [dict midashi okuri-char selected]
  (let [key (if okuri-char (str midashi okuri-char) midashi)
        section (if okuri-char :okuri-ari :okuri-nasi)
        current (get-in dict [section key] [])
        new-cands (if (seq current)
                    (vec (cons selected (remove #(= % selected) current)))
                    [selected])]
    (assoc-in dict [section key] new-cands)))


(defn- format-entry
  "Serialize a single dictionary entry."
  [[midashi cands]]
  (str midashi " /" (clojure.string/join "/" cands) "/"))


(defn- format-section
  "Serialize a section map into lines."
  [entries]
  (clojure.string/join "\n" (map format-entry entries)))


(defn serialize
  "Serialize parsed dictionary into SKK text format."
  [dict]
  (str ";; okuri-ari entries.\n"
       (format-section (:okuri-ari dict)) "\n"
       ";; okuri-nasi entries.\n"
       (format-section (:okuri-nasi dict)) "\n"))


(defn save
  "Save parsed dictionary to path atomically with a backup.

  Writes to a temporary file, backs up the existing file if present, then
  moves the temporary file into place."
  [path dict]
  (let [target (io/file path)
        temp (io/file (str path ".tmp"))
        backup (io/file (str path ".bak"))
        content (serialize dict)]
    (io/make-parents target)
    (spit temp content)
    (when (.exists target)
      (io/copy target backup))
    (io/copy temp target)
    (.delete temp)))
