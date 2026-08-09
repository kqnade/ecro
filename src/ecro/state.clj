(ns ecro.state
  (:require
    [clojure.string :as str]
    [ecro.buffer :as buffer]
    [ecro.kill-ring :as kr]
    [ecro.skk.sources :as skk-sources]
    [ecro.window :as window]))


(defn initial-state
  "Create initial editor state."
  [keymap]
  (let [scratch (buffer/make-buffer "*scratch*")]
    {:running true
     :key-sequence []
     :keymap keymap
     :frame (window/make-frame (window/make-window scratch))
     :current-buffer scratch
     :buffers [scratch]
     :kill-ring (kr/make-kill-ring)
     :notification nil
     :message nil
     :minibuffer nil
     :skk-lookup-fn (skk-sources/default-lookup)}))


(defn add-buffer
  "Add a buffer to the editor state's buffer list."
  [state buf]
  (update state :buffers conj buf))


(defn- same-buffer?
  [buffer-a buffer-b]
  (if (and (:id buffer-a) (:id buffer-b))
    (= (:id buffer-a) (:id buffer-b))
    (= (:name buffer-a) (:name buffer-b))))


(defn- buffer-by-id
  [state buffer-id]
  (first (filter #(= buffer-id (:id %)) (:buffers state))))


(defn assoc-current-buffer
  "Set current buffer and keep the buffer list entry synchronized."
  [state buf]
  (let [state' (assoc state :current-buffer buf)]
    (cond-> (if-not (contains? state :buffers)
              state'
              (let [bufs (:buffers state)
                    exists? (some #(same-buffer? % buf) bufs)
                    updated-bufs (mapv #(if (same-buffer? % buf) buf %) bufs)]
                (assoc state' :buffers (if exists?
                                         updated-bufs
                                         (conj updated-bufs buf)))))
      (:frame state') (update :frame window/assoc-selected-buffer buf))))


(defn- assoc-frame
  [state frame]
  (let [selected-window (window/selected-window frame)
        selected-buffer (buffer-by-id state (:buffer-id selected-window))]
    (if selected-buffer
      (assoc-current-buffer (assoc state :frame frame) selected-buffer)
      (assoc state :frame frame))))


(defn select-window
  "Select a frame window and synchronize its buffer with editor state."
  [state target-window]
  (let [frame (window/select-window (:frame state) target-window)
        selected-window (window/selected-window frame)
        selected-buffer (buffer-by-id state (:buffer-id selected-window))]
    (if (and (:id target-window)
             (= (:id target-window) (:id selected-window))
             selected-buffer)
      (assoc-current-buffer (assoc state :frame frame) selected-buffer)
      state)))


(defn delete-window
  "Delete a window and synchronize the selected buffer with editor state."
  [state target-window]
  (assoc-frame state (window/delete-window (:frame state) target-window)))


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
  (if-let [killed-buffer (first (filter #(= (:name %) name) (:buffers state)))]
    (let [bufs (filterv #(not (same-buffer? % killed-buffer)) (:buffers state))]
      (if (empty? bufs)
        (assoc state :message "Can't kill last buffer")
        (let [current-buffer (:current-buffer state)
              replacement-buffer (or (first (filter #(same-buffer? % current-buffer) bufs))
                                     (first bufs))
              updated-state (cond-> (assoc state :buffers bufs)
                              (:frame state)
                              (update :frame window/replace-buffer killed-buffer replacement-buffer))]
          (if (same-buffer? current-buffer killed-buffer)
            (assoc-current-buffer updated-state replacement-buffer)
            updated-state))))
    state))


(defn get-buffer-names
  "Return list of all buffer names."
  [state]
  (map :name (:buffers state)))


(defn list-buffers
  "Create or update a *Buffer List* buffer with all buffer names
   except the *Buffer List* buffer itself."
  [state]
  (let [names (remove #{"*Buffer List*"} (get-buffer-names state))
        content (str/join "\n" names)
        buf (or (first (filter #(= (:name %) "*Buffer List*") (:buffers state)))
                (buffer/make-buffer "*Buffer List*"))
        updated (assoc buf :text content :point 0)]
    (-> (assoc-current-buffer state updated)
        (assoc :message (str (count names) " buffers")))))
