(ns ecro.main
  (:require
    [ecro.bindings :as bindings]
    [ecro.key :as key]
    [ecro.kill-ring :as kr]
    [ecro.native :as native]
    [ecro.render :as render]
    [ecro.state :as state]))


(def lead-key bindings/lead-key)


(def make-keymap bindings/make-keymap)


(def default-keymap bindings/default-keymap)


(defonce editor-state
  (atom (assoc (state/initial-state default-keymap)
               :current-buffer nil
               :buffers []
               :kill-ring (kr/make-kill-ring))))


(def add-buffer state/add-buffer)


(def assoc-current-buffer state/assoc-current-buffer)


(def switch-to-buffer state/switch-to-buffer)


(def kill-buffer state/kill-buffer)


(def get-buffer-names state/get-buffer-names)


(def expand-tabs render/expand-tabs)


(def update-screen-line render/update-screen-line)


(def screen-line render/screen-line)


(def status-line render/status-line)


(def render render/render)


(def key-name key/key-name)


(def handle-key key/handle-key)


(def process-event key/process-event)


(defn -main
  "Main entry point for ecro editor."
  [& args]
  (try
    (native/init)
    (native/enable-raw-mode)
    (native/enter-alternate-screen)

    (let [state (atom (state/initial-state default-keymap))]
      (render @state)

      (loop [last-state @state]
        (when (:running last-state)
          (let [event (native/read-event)]
            (when event
              (let [new-state (swap! state process-event event)]
                (render new-state)
                (recur new-state)))))))

    (finally
      (native/leave-alternate-screen)
      (native/disable-raw-mode)
      (native/shutdown))))
