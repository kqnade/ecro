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
