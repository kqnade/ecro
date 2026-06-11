(ns ecro.undo)


(defn record-operation
  "Record an operation onto the undo stack and clear redo stack."
  [buf op]
  (-> buf
      (update :undo-stack conj op)
      (assoc :redo-stack [])))


(defn- apply-undo
  "Apply a single undo operation to buffer."
  [buf op]
  (let [text (:text buf)
        point (:point buf)]
    (case (:type op)
      :insert (let [op-point (:point op)]
                (-> buf
                    (assoc :text (str (subs text 0 op-point) (subs text (inc op-point)))
                           :point op-point)
                    (update :undo-stack pop)
                    (update :redo-stack conj op)))
      :insert-text (let [op-point (:point op)
                         txt (:text op)]
                     (-> buf
                         (assoc :text (str (subs text 0 op-point) (subs text (+ op-point (count txt))))
                                :point op-point)
                         (update :undo-stack pop)
                         (update :redo-stack conj op)))
      :delete (let [op-point (:point op)
                    ch (:char op)]
                (-> buf
                    (assoc :text (str (subs text 0 op-point) ch (subs text op-point))
                           :point (inc op-point))
                    (update :undo-stack pop)
                    (update :redo-stack conj op)))
      :delete-text (let [op-point (:point op)
                         txt (:text op)]
                     (-> buf
                         (assoc :text (str (subs text 0 op-point) txt (subs text op-point))
                                :point (+ op-point (count txt)))
                         (update :undo-stack pop)
                         (update :redo-stack conj op))))))


(defn- apply-redo
  "Apply a single redo operation to buffer."
  [buf op]
  (let [text (:text buf)
        point (:point buf)]
    (case (:type op)
      :insert (let [op-point (:point op)
                    ch (:char op)]
                (-> buf
                    (assoc :text (str (subs text 0 op-point) ch (subs text op-point))
                           :point (inc op-point))
                    (update :redo-stack pop)
                    (update :undo-stack conj op)))
      :insert-text (let [op-point (:point op)
                         txt (:text op)]
                     (-> buf
                         (assoc :text (str (subs text 0 op-point) txt (subs text op-point))
                                :point (+ op-point (count txt)))
                         (update :redo-stack pop)
                         (update :undo-stack conj op)))
      :delete (let [op-point (:point op)]
                (-> buf
                    (assoc :text (str (subs text 0 op-point) (subs text (inc op-point)))
                           :point op-point)
                    (update :redo-stack pop)
                    (update :undo-stack conj op)))
      :delete-text (let [op-point (:point op)
                         txt (:text op)]
                     (-> buf
                         (assoc :text (str (subs text 0 op-point) (subs text (+ op-point (count txt))))
                                :point op-point)
                         (update :redo-stack pop)
                         (update :undo-stack conj op))))))


(defn undo
  "Undo the last operation."
  [buf]
  (if (seq (:undo-stack buf))
    (apply-undo buf (peek (:undo-stack buf)))
    buf))


(defn redo
  "Redo the last undone operation."
  [buf]
  (if (seq (:redo-stack buf))
    (apply-redo buf (peek (:redo-stack buf)))
    buf))
