(ns ecro.core
  (:require
    [ecro.buffer :as b]))


;; Emacs basic commands

(defn forward-char
  "Move point forward one character (C-f)."
  [buf]
  (b/move-point-forward buf))


(defn backward-char
  "Move point backward one character (C-b)."
  [buf]
  (b/move-point-backward buf))


(defn next-line
  "Move point to the next line (C-n)."
  [buf]
  (let [text (:text buf)
        point (:point buf)
        current-line-col (b/point-to-line-column buf point)
        current-line (first current-line-col)
        current-col (second current-line-col)
        lines (clojure.string/split text #"\n" -1)
        next-line-idx (inc current-line)]
    (if (< next-line-idx (count lines))
      (let [next-line-text (nth lines next-line-idx)
            target-col (min current-col (count next-line-text))
            new-point (reduce + (map (comp inc count) (take next-line-idx lines)))]
        (assoc buf :point (+ new-point target-col)))
      buf)))


(defn previous-line
  "Move point to the previous line (C-p)."
  [buf]
  (let [text (:text buf)
        point (:point buf)
        current-line-col (b/point-to-line-column buf point)
        current-line (first current-line-col)
        current-col (second current-line-col)
        lines (clojure.string/split text #"\n" -1)]
    (if (> current-line 0)
      (let [prev-line-idx (dec current-line)
            prev-line-text (nth lines prev-line-idx)
            target-col (min current-col (count prev-line-text))
            new-point (reduce + (map (comp inc count) (take prev-line-idx lines)))]
        (assoc buf :point (+ new-point target-col)))
      buf)))


(defn move-beginning-of-line
  "Move point to beginning of current line (C-a)."
  [buf]
  (let [point (:point buf)
        text (:text buf)
        lines (clojure.string/split text #"\n" -1)
        line-col (b/point-to-line-column buf point)
        line-idx (first line-col)]
    (assoc buf :point (reduce + (map inc (take line-idx lines))))))


(defn move-end-of-line
  "Move point to end of current line (C-e)."
  [buf]
  (let [point (:point buf)
        text (:text buf)
        lines (clojure.string/split text #"\n" -1)
        line-col (b/point-to-line-column buf point)
        line-idx (first line-col)
        line-text (nth lines line-idx)]
    (assoc buf :point (+ (reduce + (map inc (take line-idx lines))) (count line-text)))))


(defn kill-line
  "Kill from point to end of line (C-k)."
  [buf]
  (let [point (:point buf)
        text (:text buf)
        lines (clojure.string/split text #"\n" -1)
        line-col (b/point-to-line-column buf point)
        line-idx (first line-col)
        col (second line-col)
        line-text (nth lines line-idx)]
    (assoc buf :text (str (subs text 0 point) (subs text (+ point (- (count line-text) col)))))))


(defn- word-char?
  "Return true if ch is a word constituent character."
  [ch]
  (boolean (re-matches #"[\w-]" (str ch))))


(defn- skip-while
  "Advance point while pred is true. Returns new point."
  [text point pred]
  (loop [p point]
    (if (and (< p (count text)) (pred (get text p)))
      (recur (inc p))
      p)))


(defn- skip-while-back
  "Move point backward while pred is true. Returns new point."
  [text point pred]
  (loop [p point]
    (if (and (>= p 0) (pred (get text p)))
      (recur (dec p))
      p)))


(defn forward-word
  "Move point forward to the end of the next word (M-f)."
  [buf]
  (let [text (:text buf)
        point (:point buf)
        p1 (skip-while text point #(not (word-char? %)))
        p2 (skip-while text p1 word-char?)]
    (assoc buf :point p2)))


(defn backward-word
  "Move point backward to the beginning of the previous word (M-b)."
  [buf]
  (let [text (:text buf)
        point (:point buf)
        p1 (skip-while-back text (dec point) #(not (word-char? %)))
        p2 (skip-while-back text p1 word-char?)]
    (assoc buf :point (max 0 (inc p2)))))


(defn beginning-of-buffer
  "Move point to the beginning of the buffer (M-<)."
  [buf]
  (assoc buf :point 0))


(defn end-of-buffer
  "Move point to the end of the buffer (M->)."
  [buf]
  (assoc buf :point (count (:text buf))))


(defn set-mark-command
  "Set or deactivate the mark (C-SPC).
   First call sets the mark; second consecutive call deactivates it."
  [buf]
  (if (:mark buf)
    (b/deactivate-mark buf)
    (b/set-mark buf)))
