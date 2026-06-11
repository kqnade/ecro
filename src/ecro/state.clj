(ns ecro.state
  (:require
    [ecro.buffer :as buffer]
    [ecro.kill-ring :as kr]))


(defn initial-state
  "Create initial editor state."
  [keymap]
  (let [scratch (buffer/make-buffer "*scratch*")]
    {:running true
     :key-sequence []
     :keymap keymap
     :frame nil
     :current-buffer scratch
     :buffers [scratch]
     :kill-ring (kr/make-kill-ring)
     :notification nil
     :message nil
     :minibuffer nil}))


(defn add-buffer
  "Add a buffer to the editor state's buffer list."
  [state buf]
  (update state :buffers conj buf))


(defn assoc-current-buffer
  "Set current buffer and keep the buffer list entry synchronized."
  [state buf]
  (let [state' (assoc state :current-buffer buf)]
    (if-not (contains? state :buffers)
      state'
      (let [bufs (:buffers state)
            exists? (some #(= (:name %) (:name buf)) bufs)
            updated-bufs (mapv #(if (= (:name %) (:name buf)) buf %) bufs)]
        (assoc state' :buffers (if exists?
                                 updated-bufs
                                 (conj updated-bufs buf)))))))


(defn switch-to-buffer
  "Switch current buffer by name. Creates new buffer if not found."
  [state name]
  (if-let [buf (first (filter #(= (:name %) name) (:buffers state)))]
    (assoc-current-buffer state buf)
    (let [new-buf (buffer/make-buffer name)]
      (assoc-current-buffer state new-buf))))


(defn kill-buffer
  "Kill buffer by name. Switches to another buffer if killing current."
  [state name]
  (let [bufs (filterv #(not= (:name %) name) (:buffers state))]
    (if (empty? bufs)
      (assoc state :message "Can't kill last buffer")
      (let [current-name (:name (:current-buffer state))]
        (cond-> (assoc state :buffers bufs)
          (= current-name name)
          (assoc-current-buffer (first bufs)))))))


(defn get-buffer-names
  "Return list of all buffer names."
  [state]
  (map :name (:buffers state)))
