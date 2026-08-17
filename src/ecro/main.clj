(ns ecro.main
  (:gen-class)
  (:require
    [ecro.bindings :as bindings]
    [ecro.key :as key]
    [ecro.native :as native]
    [ecro.render :as render]
    [ecro.state :as state]))


(def lead-key bindings/lead-key)


(def make-keymap bindings/make-keymap)


(def default-keymap bindings/default-keymap)


(defonce editor-state
  (atom (state/initial-state default-keymap)))


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


(defn smoke-test
  "Verify that the Rust terminal adapter can be initialized and shut down."
  []
  (let [init-result (native/init)]
    (when-not (= 0 init-result)
      (throw (IllegalStateException.
               (str "Native smoke test initialization failed: " init-result)))))
  (let [shutdown-result (native/shutdown)]
    (when-not (= 0 shutdown-result)
      (throw (IllegalStateException.
               (str "Native smoke test shutdown failed: " shutdown-result))))))


(defn -main
  "Main entry point for ecro editor."
  [& args]
  (if (= ["--smoke-test"] args)
    (smoke-test)
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
        (native/shutdown)))))
