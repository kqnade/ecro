(ns ecro.skk.henkan
  "Conversion logic for SKK.

  Manages henkan start, dictionary lookup, candidate cycling, confirmation, and
  cancellation."
  (:require
    [ecro.buffer :as buffer]
    [ecro.skk.jisyo :as jisyo]
    [ecro.skk.state :as skk-state]))


(defn- current-midashi
  "Extract the hiragana midashi between henkan-start and point."
  [buffer]
  (let [start (skk-state/henkan-start buffer)
        end (:point buffer)
        text (:text buffer)]
    (when (and start (<= start end))
      (subs text start end))))


(defn- replace-range
  "Replace text in buffer from start to end with new-text and update point."
  [buffer start end new-text]
  (let [text (:text buffer)
        before (subs text 0 start)
        after (subs text end)]
    (-> buffer
        (assoc :text (str before new-text after))
        (assoc :point (+ start (count new-text))))))


(defn- lookup-candidates
  "Search dictionaries for candidates.

  For okuri-ari conversion, okuri-char is appended to the midashi."
  [dict midashi okuri-char]
  (if okuri-char
    (jisyo/candidates dict midashi okuri-char)
    (jisyo/candidates dict midashi)))


(defn set-henkan-key
  "Store the original midashi before conversion for cancellation."
  [buffer midashi]
  (assoc-in buffer [:skk :henkan-key] midashi))


(defn start
  "Start conversion from henkan-on mode using SPC.

  Looks up candidates for the current midashi. If found, replaces the midashi
  with the first candidate and enters active conversion. Returns updated buffer
  and a status message."
  [buffer dict]
  (let [midashi (current-midashi buffer)
        okuri-char (get-in buffer [:skk :okuri-char])
        base-midashi (if okuri-char
                       (subs midashi 0 (- (count midashi) (count okuri-char)))
                       midashi)
        cands (lookup-candidates dict base-midashi okuri-char)]
    (if (seq cands)
      (let [start (skk-state/henkan-start buffer)
            end (:point buffer)
            after-replace (replace-range buffer start end (first cands))]
        (-> after-replace
            (skk-state/set-henkan-mode :active)
            (skk-state/set-henkan-end (+ start (count (first cands))))
            (skk-state/set-candidates cands)
            (set-henkan-key base-midashi)))
      (-> buffer
          (skk-state/clear-henkan)
          (assoc :notification {:level :warn :message (str "No SKK candidates: " midashi)})))))


(defn cycle-next
  "Cycle to the next candidate in active conversion."
  [buffer]
  (if (skk-state/active-conversion? buffer)
    (let [cands (skk-state/candidates buffer)
          idx (mod (inc (skk-state/candidate-index buffer)) (count cands))
          start (skk-state/henkan-start buffer)
          end (skk-state/henkan-end buffer)
          after-replace (replace-range buffer start end (nth cands idx))]
      (-> after-replace
          (skk-state/set-henkan-end (+ start (count (nth cands idx))))
          (skk-state/set-candidate-index idx)))
    buffer))


(defn cycle-previous
  "Cycle to the previous candidate in active conversion."
  [buffer]
  (if (skk-state/active-conversion? buffer)
    (let [cands (skk-state/candidates buffer)
          idx (mod (dec (skk-state/candidate-index buffer)) (count cands))
          start (skk-state/henkan-start buffer)
          end (skk-state/henkan-end buffer)
          after-replace (replace-range buffer start end (nth cands idx))]
      (-> after-replace
          (skk-state/set-henkan-end (+ start (count (nth cands idx))))
          (skk-state/set-candidate-index idx)))
    buffer))


(defn selected-candidate
  "Return the currently selected candidate, or nil if not in active conversion."
  [buffer]
  (when (skk-state/active-conversion? buffer)
    (let [cands (skk-state/candidates buffer)
          idx (skk-state/candidate-index buffer)]
      (nth cands idx))))


(defn confirm
  "Confirm the current candidate and clear henkan state."
  [buffer]
  (if (or (skk-state/active-conversion? buffer)
          (skk-state/henkan-on? buffer))
    (skk-state/clear-henkan buffer)
    buffer))


(defn cancel
  "Cancel active conversion or henkan input.

  Active conversion: restore original kana.
  Henkan-on: keep kana text and clear henkan state.
  Pending prefix: clear prefix."
  [buffer]
  (cond
    (skk-state/active-conversion? buffer)
    (let [start (skk-state/henkan-start buffer)
          end (skk-state/henkan-end buffer)
          midashi (get-in buffer [:skk :henkan-key] "")
          after-restore (replace-range buffer start end midashi)]
      (skk-state/clear-henkan after-restore))

    (skk-state/henkan-on? buffer)
    (skk-state/clear-henkan buffer)

    :else
    (skk-state/cancel-prefix buffer)))
