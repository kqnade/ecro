(ns ecro.skk.ui
  "Status line and notification helpers for SKK."
  (:require
    [ecro.skk.state :as skk-state]))


(defn- mode-label
  "Return a short label for the current SKK input mode."
  [buffer]
  (case (skk-state/mode buffer)
    :hiragana "かな"
    :katakana "カナ"
    :latin "latin"
    ""))


(defn- candidate-label
  "Return a candidate count label like (1/5)."
  [buffer]
  (let [cands (skk-state/candidates buffer)
        idx (skk-state/candidate-index buffer)]
    (str "(" (inc idx) "/" (count cands) ")")))


(defn status-message
  "Return a status line string for the current SKK state.

  - Disabled: nil
  - Hiragana/katakana/latin: SKK:<mode>
  - Henkan reading: SKK:▽ <midashi>
  - Active conversion: SKK:▼ <candidate> (index/count)"
  [buffer]
  (when (skk-state/enabled? buffer)
    (cond
      (skk-state/active-conversion? buffer)
      (let [cands (skk-state/candidates buffer)
            idx (skk-state/candidate-index buffer)]
        (str "SKK:▼ " (nth cands idx) " " (candidate-label buffer)))

      (skk-state/henkan-on? buffer)
      (let [text (:text buffer)
            start (skk-state/henkan-start buffer)
            end (:point buffer)
            midashi (when (and start (<= start end))
                      (subs text start end))]
        (str "SKK:▽ " midashi))

      :else
      (str "SKK:" (mode-label buffer)))))


(defn candidate-list-message
  "Return a notification message showing all candidates with current marked.

  Example: 'SKK candidates: ▼日本 □二本 □...'"
  [buffer]
  (when (skk-state/active-conversion? buffer)
    (let [cands (skk-state/candidates buffer)
          idx (skk-state/candidate-index buffer)
          formatted (map-indexed
                      (fn [i c]
                        (if (= i idx)
                          (str "▼" c)
                          (str "□" c)))
                      cands)]
      (str "SKK: " (clojure.string/join " " formatted)))))
