(ns ecro.skk.state
  "Buffer-local SKK state and lifecycle.")


(defn default-state
  "Return a fresh SKK state map."
  []
  {:mode :hiragana
   :henkan-mode nil
   :kana-prefix ""
   :henkan-start nil
   :henkan-end nil
   :henkan-key nil
   :okuri-char nil
   :okurigana ""
   :candidates []
   :candidate-index 0})


(defn enabled?
  "Return true if SKK is enabled for the buffer."
  [buffer]
  (contains? (:minor-modes buffer) :skk-mode))


(defn ensure-state
  "Ensure the buffer has a :skk state map."
  [buffer]
  (if (:skk buffer)
    buffer
    (assoc buffer :skk (default-state))))


(defn toggle
  "Toggle SKK minor mode and initialize/clear state."
  [buffer]
  (if (enabled? buffer)
    (-> buffer
        (update :minor-modes disj :skk-mode)
        (dissoc :skk))
    (-> buffer
        (update :minor-modes (fnil conj #{}) :skk-mode)
        ensure-state)))


(defn mode
  "Return the current SKK input mode."
  [buffer]
  (get-in buffer [:skk :mode]))


(defn set-mode
  "Set the SKK input mode."
  [buffer new-mode]
  (assoc-in buffer [:skk :mode] new-mode))


(defn kana-prefix
  "Return the current pending romaji prefix."
  [buffer]
  (get-in buffer [:skk :kana-prefix] ""))


(defn set-kana-prefix
  "Set the pending romaji prefix."
  [buffer prefix]
  (assoc-in buffer [:skk :kana-prefix] prefix))


(defn henkan-mode
  "Return the current henkan mode."
  [buffer]
  (get-in buffer [:skk :henkan-mode]))


(defn set-henkan-mode
  "Set the henkan mode."
  [buffer mode]
  (assoc-in buffer [:skk :henkan-mode] mode))


(defn henkan-start
  "Return the henkan start offset."
  [buffer]
  (get-in buffer [:skk :henkan-start]))


(defn set-henkan-start
  "Set the henkan start offset."
  [buffer point]
  (assoc-in buffer [:skk :henkan-start] point))


(defn henkan-end
  "Return the henkan end offset."
  [buffer]
  (get-in buffer [:skk :henkan-end]))


(defn set-henkan-end
  "Set the henkan end offset."
  [buffer point]
  (assoc-in buffer [:skk :henkan-end] point))


(defn candidates
  "Return conversion candidates."
  [buffer]
  (get-in buffer [:skk :candidates] []))


(defn set-candidates
  "Set conversion candidates and reset index."
  [buffer cands]
  (-> buffer
      (assoc-in [:skk :candidates] cands)
      (assoc-in [:skk :candidate-index] 0)))


(defn candidate-index
  "Return current candidate index."
  [buffer]
  (get-in buffer [:skk :candidate-index] 0))


(defn set-candidate-index
  "Set current candidate index."
  [buffer idx]
  (assoc-in buffer [:skk :candidate-index] idx))


(defn clear-henkan
  "Clear henkan state but keep SKK mode."
  [buffer]
  (-> buffer
      (assoc-in [:skk :henkan-mode] nil)
      (assoc-in [:skk :henkan-start] nil)
      (assoc-in [:skk :henkan-end] nil)
      (assoc-in [:skk :henkan-key] nil)
      (assoc-in [:skk :okuri-char] nil)
      (assoc-in [:skk :okurigana] "")
      (assoc-in [:skk :candidates] [])
      (assoc-in [:skk :candidate-index] 0)))


(defn cancel-prefix
  "Clear pending kana prefix."
  [buffer]
  (assoc-in buffer [:skk :kana-prefix] ""))


(defn active-conversion?
  "Return true if a candidate is currently being shown."
  [buffer]
  (= :active (henkan-mode buffer)))


(defn henkan-on?
  "Return true if reading input for conversion."
  [buffer]
  (= :on (henkan-mode buffer)))
