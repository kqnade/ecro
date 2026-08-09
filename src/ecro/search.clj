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


(defn isearch-delete-char
  "Remove the last character from the i-search pattern."
  [state]
  (update state :pattern
          (fn [^String pattern]
            (if (empty? pattern)
              pattern
              (subs pattern 0 (.offsetByCodePoints pattern (count pattern) -1))))))


(defn isearch-execute
  "Execute i-search with current pattern. Returns updated buffer."
  [state buf]
  (let [pattern (:pattern state)
        start-point (or (:start-point state) (:point buf))
        anchor-point (:anchor-point state)
        search-point (if (some? anchor-point)
                       (if (= :backward (:direction state))
                         (inc anchor-point)
                         anchor-point)
                       start-point)
        fallback-point (or anchor-point start-point)]
    (if (seq pattern)
      (let [result (case (:direction state)
                     :forward (search-forward (assoc buf :point search-point) pattern)
                     :backward (search-backward (assoc buf :point search-point) pattern))]
        (or result (assoc buf :point fallback-point)))
      (assoc buf :point fallback-point))))


(defn isearch-repeat
  "Repeat the current i-search from the current match in direction."
  [state buf direction]
  (let [pattern (:pattern state)
        point (:point buf)
        result (when (seq pattern)
                 (case direction
                   :forward (search-forward (assoc buf :point (inc point)) pattern)
                   :backward (search-backward (assoc buf :point point) pattern)))]
    (or result buf)))


(defn isearch-cancel
  "Cancel i-search and restore original point."
  [state buf]
  (if-let [start (:start-point state)]
    (assoc buf :point start)
    buf))
