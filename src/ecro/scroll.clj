(ns ecro.scroll
  (:require
    [ecro.buffer :as b]))


(defn scroll-down
  "Scroll down by n lines."
  [buf n]
  (update buf :scroll-line + n))


(defn scroll-up
  "Scroll up by n lines."
  [buf n]
  (update buf :scroll-line (fn [s] (max 0 (- s n)))))


(defn scroll-up-command
  "Scroll forward by one screen, showing the next page (C-v)."
  [buf window-height]
  (scroll-down buf window-height))


(defn scroll-down-command
  "Scroll backward by one screen, showing the previous page (M-v)."
  [buf window-height]
  (scroll-up buf window-height))


(defn adjust-scroll-for-point
  "Adjust scroll position to keep cursor visible."
  [buf height]
  (let [point (:point buf 0)
        [line _] (b/point-to-line-column buf point)
        scroll-line (:scroll-line buf 0)]
    (cond
      ;; Point above visible area
      (< line scroll-line)
      (assoc buf :scroll-line line)

      ;; Point below visible area
      (>= line (+ scroll-line height))
      (assoc buf :scroll-line (- line height -1))

      :else
      buf)))
