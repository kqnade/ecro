(ns ecro.buffer)

(defn make-buffer
  "Create a new buffer with the given name."
  [name]
  {:name name
   :text ""
   :point 0})

(defn insert-char
  "Insert a character at the current point and advance point."
  [buf ch]
  (let [point (:point buf)
        text (:text buf)]
    (assoc buf
           :text (str (subs text 0 point) ch (subs text point))
           :point (inc point))))

(defn delete-char-forward
  "Delete the character at the current point."
  [buf]
  (let [point (:point buf)
        text (:text buf)]
    (if (< point (count text))
      (assoc buf :text (str (subs text 0 point) (subs text (inc point))))
      buf)))

(defn move-point-forward
  "Move point forward by one character if not at end."
  [buf]
  (let [point (:point buf)
        text (:text buf)]
    (if (< point (count text))
      (assoc buf :point (inc point))
      buf)))

(defn move-point-backward
  "Move point backward by one character if not at beginning."
  [buf]
  (let [point (:point buf)]
    (if (> point 0)
      (assoc buf :point (dec point))
      buf)))

(defn point-to-line-column
  "Convert a point (0-indexed character position) to [line column]."
  [buf point]
  (let [text (:text buf)
        lines (clojure.string/split (subs text 0 point) #"\n" -1)]
    [(dec (count lines))
     (count (last lines))]))
