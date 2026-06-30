(ns ecro.skk.input
  "Handle printable key input while SKK mode is active.

  This namespace converts romaji to kana, toggles submodes, and starts henkan
  when uppercase letters are typed."
  (:require
    [ecro.buffer :as buffer]
    [ecro.skk.kana :as kana]
    [ecro.skk.state :as skk-state]))


(defn- hiragana?
  [buffer]
  (= :hiragana (skk-state/mode buffer)))


(defn- katakana?
  [buffer]
  (= :katakana (skk-state/mode buffer)))


(defn- latin?
  [buffer]
  (= :latin (skk-state/mode buffer)))


(defn- step-fn
  "Return the appropriate step function for the current mode."
  [buffer]
  (if (katakana? buffer)
    kana/step-katakana
    kana/step))


(defn- flush-fn
  "Return the appropriate flush function for the current mode."
  [buffer]
  (if (katakana? buffer)
    kana/katakana-flush-prefix
    kana/flush-prefix))


(defn- insert-kana
  "Insert kana string into buffer at point."
  [buffer kana-str]
  (reduce buffer/insert-char buffer kana-str))


(defn- handle-submode-toggle
  "Handle q and l mode switching keys."
  [buffer ch]
  (case ch
    \q (if (katakana? buffer)
         (skk-state/set-mode buffer :hiragana)
         (skk-state/set-mode buffer :katakana))
    \l (skk-state/set-mode buffer :latin)
    buffer))


(declare handle-char)


(defn- process-emit
  "Process an emit result from the kana stepper."
  [buffer result]
  (let [kana-str (:kana result)
        next-prefix (:prefix result)
        retry-char (:retry result)]
    (if retry-char
      (let [after-insert (insert-kana buffer kana-str)
            after-prefix (skk-state/set-kana-prefix after-insert "")]
        (handle-char after-prefix retry-char))
      (-> buffer
          (insert-kana kana-str)
          (skk-state/set-kana-prefix next-prefix)))))


(defn- handle-kana-char
  "Process a lowercase character in hiragana/katakana mode."
  [buffer ch]
  (let [tree kana/base-tree
        prefix (skk-state/kana-prefix buffer)
        stepper (step-fn buffer)
        result (stepper tree prefix ch)]
    (case (:state result)
      :wait (skk-state/set-kana-prefix buffer (:prefix result))
      :emit (process-emit buffer result)
      :noop (-> buffer
                (skk-state/cancel-prefix)
                (skk-state/set-mode :latin))
      buffer)))


(defn- start-henkan
  "Start henkan mode at current point and insert the first kana."
  [buffer ch]
  (let [point (:point buffer)
        lower (Character/toLowerCase ^Character ch)
        after-start (-> buffer
                        (skk-state/set-henkan-mode :on)
                        (skk-state/set-henkan-start point))]
    (handle-kana-char after-start lower)))


(defn- handle-uppercase
  "Handle uppercase letter in hiragana/katakana mode."
  [buffer ch]
  (cond
    (skk-state/henkan-on? buffer)
    (let [lower (Character/toLowerCase ^Character ch)]
      (-> buffer
          (skk-state/set-henkan-mode :on)
          (assoc-in [:skk :okuri-char] (str lower))
          (handle-kana-char lower)))

    (skk-state/active-conversion? buffer)
    buffer

    :else
    (start-henkan buffer ch)))


(defn handle-char
  "Process a single character while SKK is active. Returns updated buffer."
  [buffer ch]
  (cond
    (= :latin (skk-state/mode buffer))
    (cond
      (= ch \newline) (skk-state/set-mode buffer :hiragana)
      :else (buffer/insert-char buffer ch))

    (#{\q \l} ch)
    (handle-submode-toggle buffer ch)

    (and (Character/isUpperCase ^Character ch)
         (Character/isLetter ^Character ch))
    (handle-uppercase buffer ch)

    (and (Character/isLowerCase ^Character ch)
         (Character/isLetter ^Character ch))
    (handle-kana-char buffer ch)

    :else
    (buffer/insert-char buffer ch)))


(defn flush-prefix
  "Flush any pending kana prefix into the buffer.

  If the prefix corresponds to a rule, emit its kana. Otherwise emit the
  prefix as raw ASCII characters."
  [buffer]
  (let [prefix (skk-state/kana-prefix buffer)]
    (if (seq prefix)
      (let [[kana-str _] ((flush-fn buffer) kana/base-tree prefix)]
        (if kana-str
          (-> buffer
              (insert-kana kana-str)
              (skk-state/cancel-prefix))
          (-> buffer
              (insert-kana prefix)
              (skk-state/cancel-prefix))))
      buffer)))
