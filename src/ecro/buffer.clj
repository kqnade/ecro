(ns ecro.buffer)


(defn make-buffer
  "Create a new buffer with the given name."
  [name]
  {:name name
   :text ""
   :point 0
   :undo-stack []
   :redo-stack []})


(defn- record-operation
  "Record an operation onto the undo stack and clear redo stack."
  [buf op]
  (-> buf
      (update :undo-stack conj op)
      (assoc :redo-stack [])))


(defn insert-char
  "Insert a character at the current point and advance point."
  [buf ch]
  (let [point (:point buf)
        text (:text buf)
        new-buf (assoc buf
                       :text (str (subs text 0 point) ch (subs text point))
                       :point (inc point))]
    (record-operation new-buf {:type :insert
                               :char ch
                               :point point})))


(defn delete-char-forward
  "Delete the character at the current point."
  [buf]
  (let [point (:point buf)
        text (:text buf)]
    (if (< point (count text))
      (let [deleted-char (get text point)
            new-buf (assoc buf :text (str (subs text 0 point) (subs text (inc point))))]
        (record-operation new-buf {:type :delete
                                   :char deleted-char
                                   :point point}))
      buf)))


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
        (record-operation new-buf {:type :delete
                                   :char deleted-char
                                   :point (dec point)}))
      buf)))


(defn insert-newline
  "Insert a newline at the current point."
  [buf]
  (insert-char buf \newline))


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


(defn undo
  "Undo the last operation."
  [buf]
  (if (seq (:undo-stack buf))
    (let [op (peek (:undo-stack buf))
          text (:text buf)
          point (:point buf)]
      (case (:type op)
        :insert (let [op-point (:point op)]
                  (-> buf
                      (assoc :text (str (subs text 0 op-point) (subs text (inc op-point)))
                             :point op-point)
                      (update :undo-stack pop)
                      (update :redo-stack conj op)))
        :delete (let [op-point (:point op)
                      ch (:char op)]
                  (-> buf
                      (assoc :text (str (subs text 0 op-point) ch (subs text op-point))
                             :point (inc op-point))
                      (update :undo-stack pop)
                      (update :redo-stack conj op)))))
    buf))


(defn redo
  "Redo the last undone operation."
  [buf]
  (if (seq (:redo-stack buf))
    (let [op (peek (:redo-stack buf))
          text (:text buf)
          point (:point buf)]
      (case (:type op)
        :insert (let [op-point (:point op)
                      ch (:char op)]
                  (-> buf
                      (assoc :text (str (subs text 0 op-point) ch (subs text op-point))
                             :point (inc op-point))
                      (update :redo-stack pop)
                      (update :undo-stack conj op)))
        :delete (let [op-point (:point op)]
                  (-> buf
                      (assoc :text (str (subs text 0 op-point) (subs text (inc op-point)))
                             :point op-point)
                      (update :redo-stack pop)
                      (update :undo-stack conj op)))))
    buf))
