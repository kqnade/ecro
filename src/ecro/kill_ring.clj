(ns ecro.kill-ring
  (:require
    [ecro.buffer :as b]))


(defn make-kill-ring
  "Create a new kill ring with optional max size."
  ([] (make-kill-ring 60))
  ([max-size]
   {:entries []
    :index 0
    :max-size max-size}))


(defn kill-text
  "Add text to the kill ring."
  [kr text]
  (if (seq text)
    (let [entries (vec (take (:max-size kr) (cons text (:entries kr))))]
      (assoc kr :entries entries :index 0))
    kr))


(defn kill-append
  "Append text to the last kill."
  [kr text]
  (if (and (seq text) (seq (:entries kr)))
    (let [last-entry (first (:entries kr))
          new-entry (str last-entry text)]
      (-> kr
          (update :entries rest)
          (kill-text new-entry)))
    (kill-text kr text)))


(defn yank
  "Return the current entry from the kill ring."
  [kr]
  (when (seq (:entries kr))
    (nth (:entries kr) (:index kr) "")))


(defn yank-pop
  "Cycle to the previous entry in the kill ring."
  [kr]
  (if (seq (:entries kr))
    (let [new-index (mod (inc (:index kr)) (count (:entries kr)))]
      (assoc kr :index new-index))
    kr))


(defn kill-line
  "Kill from point to end of line. Returns [new-buffer killed-text]."
  [buf]
  (let [point (:point buf)
        text (:text buf)
        next-newline (clojure.string/index-of text "\n" point)
        end (or next-newline (count text))
        killed (subs text point end)
        new-text (str (subs text 0 point) (subs text end))]
    [(assoc buf :text new-text) killed]))


(defn kill-region
  "Kill text between mark and point. Returns [new-buffer killed-text]."
  [buf mark point]
  (let [start (min mark point)
        end (max mark point)
        text (:text buf)
        killed (subs text start end)
        new-text (str (subs text 0 start) (subs text end))]
    [(assoc buf :text new-text :point start) killed]))


(defn kill-ring-save
  "Copy region text to kill ring without deleting. Returns updated kill ring."
  [kr buf]
  (if-let [region (b/region-text buf)]
    (kill-text kr region)
    kr))


(defn yank-text
  "Insert current kill ring entry at point. Returns updated buffer."
  [buf kr]
  (if-let [text (yank kr)]
    (let [point (:point buf)
          old-text (:text buf)
          new-text (str (subs old-text 0 point) text (subs old-text point))]
      (assoc buf :text new-text :point (+ point (count text))))
    buf))
