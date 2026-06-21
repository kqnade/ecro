(ns ecro.buffer
  (:require
    [ecro.mode :as mode]
    [ecro.undo :as undo]))


(defn make-buffer
  "Create a new buffer with the given name."
  ([name]
   (make-buffer name {}))
  ([name opts]
   (mode/set-buffer-mode
     {:name name
      :text ""
      :point 0
      :mark nil
      :scroll-line 0
      :undo-stack []
      :redo-stack []
      :tab-width (get opts :tab-width 2)
      :indent-tabs-mode (get opts :indent-tabs-mode false)
      :saved-text ""})))


(defn insert-char
  "Insert a character at the current point and advance point."
  [buf ch]
  (let [point (:point buf)
        text (:text buf)
        new-buf (assoc buf
                       :text (str (subs text 0 point) ch (subs text point))
                       :point (inc point))]
    (undo/record-operation new-buf {:type :insert
                                    :char ch
                                    :point point})))


(defn insert-text
  "Insert text at current point and advance point. Records as single undo operation."
  [buf text]
  (let [point (:point buf)
        old-text (:text buf)
        new-text (str (subs old-text 0 point) text (subs old-text point))]
    (undo/record-operation (assoc buf :text new-text :point (+ point (count text)))
                           {:type :insert-text
                            :text text
                            :point point})))


(defn delete-char-forward
  "Delete the character at the current point."
  [buf]
  (let [point (:point buf)
        text (:text buf)]
    (if (< point (count text))
      (let [deleted-char (get text point)
            new-buf (assoc buf :text (str (subs text 0 point) (subs text (inc point))))]
        (undo/record-operation new-buf {:type :delete
                                        :char deleted-char
                                        :point point}))
      buf)))


(defn delete-text
  "Delete text in range [start end). Records as single undo operation."
  [buf start end]
  (let [text (:text buf)
        deleted (subs text start end)
        new-text (str (subs text 0 start) (subs text end))]
    (undo/record-operation (assoc buf :text new-text :point start :mark nil)
                           {:type :delete-text
                            :text deleted
                            :point start
                            :original-point (:point buf)})))


(defn delete-char-backward
  "Delete the character before the current point."
  [buf]
  (let [point (:point buf)]
    (if (> point 0)
      (let [text (:text buf)
            deleted-char (get text (dec point))
            new-buf (assoc buf
                           :text (str (subs text 0 (dec point)) (subs text point))
                           :point (dec point))]
        (undo/record-operation new-buf {:type :delete
                                        :char deleted-char
                                        :point (dec point)}))
      buf)))


(defn point-to-line-column
  "Convert a point (0-indexed character position) to [line column]."
  [buf point]
  (let [text (:text buf)
        lines (clojure.string/split (subs text 0 point) #"\n" -1)]
    [(dec (count lines))
     (count (last lines))]))


(defn insert-newline
  "Insert a newline at the current point."
  [buf]
  (insert-char buf \newline))


(defn insert-tab
  "Insert a tab or spaces depending on indent-tabs-mode."
  [buf]
  (let [tab-width (:tab-width buf 8)
        use-tabs (:indent-tabs-mode buf true)]
    (if use-tabs
      (insert-char buf \tab)
      (let [point (:point buf)
            text (:text buf)
            lines (clojure.string/split text #"\n" -1)
            [line _] (point-to-line-column buf point)
            line-text (nth lines line "")
            line-start (reduce + (map inc (take line lines)))
            col (- point line-start)
            spaces (- tab-width (mod col tab-width))]
        (reduce (fn [b _] (insert-char b \space)) buf (range spaces))))))


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


(defn set-mark
  "Set mark to current point."
  [buf]
  (assoc buf :mark (:point buf)))


(defn deactivate-mark
  "Clear the mark."
  [buf]
  (assoc buf :mark nil))


(defn region-text
  "Return text between mark and point, or nil if no mark."
  [buf]
  (when-let [mark (:mark buf)]
    (let [start (min mark (:point buf))
          end (max mark (:point buf))
          text (:text buf)]
      (subs text start end))))


(defn modified?
  "Return true if buffer has unsaved changes."
  [buf]
  (not= (:text buf) (:saved-text buf)))
