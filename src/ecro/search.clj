(ns ecro.search)


(defn search-forward
  "Search forward for pattern from current point. Returns updated buffer or nil."
  [buf pattern]
  (let [text (:text buf)
        point (:point buf)
        idx (clojure.string/index-of text pattern point)]
    (when idx
      (assoc buf :point idx))))


(defn search-backward
  "Search backward for pattern from current point. Returns updated buffer or nil."
  [buf pattern]
  (let [text (:text buf)
        point (:point buf)
        idx (clojure.string/last-index-of text pattern (dec point))]
    (when idx
      (assoc buf :point idx))))


(defn make-isearch
  "Create incremental search state."
  [direction]
  {:pattern ""
   :direction direction
   :start-point nil})


(defn isearch-add-char
  "Add a character to the i-search pattern."
  [state ch]
  (update state :pattern str ch))


(defn isearch-execute
  "Execute i-search with current pattern. Returns updated buffer."
  [state buf]
  (let [pattern (:pattern state)
        start-point (or (:start-point state) (:point buf))]
    (if (seq pattern)
      (let [result (case (:direction state)
                     :forward (search-forward (assoc buf :point start-point) pattern)
                     :backward (search-backward (assoc buf :point start-point) pattern))]
        (or result (assoc buf :point start-point)))
      buf)))


(defn isearch-cancel
  "Cancel i-search and restore original point."
  [state buf]
  (if-let [start (:start-point state)]
    (assoc buf :point start)
    buf))
