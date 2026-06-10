(ns ecro.core
  (:require [ecro.buffer :as b]))

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
