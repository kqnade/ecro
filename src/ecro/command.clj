(ns ecro.command
  (:require
    [ecro.buffer :as buffer]
    [ecro.core :as core]
    [ecro.file :as file]
    [ecro.kill-ring :as kr]
    [ecro.minibuffer :as minibuffer]
    [ecro.notification :as notification]
    [ecro.state :as state]
    [ecro.undo :as undo]))


(defn- save-buffer
  [editor-state buf kill-ring]
  (let [[state' new-buf] (if-let [filepath (:filepath buf)]
                           (try
                             [(notification/info editor-state (str "Wrote " filepath))
                              (assoc (or (file/save-buffer buf) buf)
                                     :saved-text (:text buf))]
                             (catch Exception e
                               [(notification/error editor-state (str "Save failed: " (.getMessage e))) buf]))
                           [(notification/warn editor-state "No file to save") buf])]
    (state/assoc-current-buffer (assoc state'
                                       :kill-ring kill-ring
                                       :key-sequence [])
                                new-buf)))


(defn execute-command
  "Execute an editor command against state."
  [editor-state command]
  (let [buf (:current-buffer editor-state)
        kill-ring (or (:kill-ring editor-state) (kr/make-kill-ring))]
    (cond
      (= command :save-buffer)
      (save-buffer editor-state buf kill-ring)

      (= command :find-file)
      (assoc editor-state
             :minibuffer (ecro.minibuffer/prompt-for "Find file: " :open-file)
             :key-sequence [])

      (= command :switch-to-buffer)
      (assoc editor-state
             :minibuffer (ecro.minibuffer/prompt-for "Switch to buffer: " :switch-to-buffer)
             :key-sequence [])

      (= command :kill-buffer)
      (assoc editor-state
             :minibuffer (ecro.minibuffer/prompt-for "Kill buffer: " :kill-buffer)
             :key-sequence [])

      (= command :write-file)
      (assoc editor-state
             :minibuffer (ecro.minibuffer/prompt-for "Write file: " :write-file)
             :key-sequence [])

      (= command :list-buffers)
      (state/list-buffers editor-state)

      :else
      (let [[new-buf new-kr] (case command
                               :forward-char [(core/forward-char buf) kill-ring]
                               :backward-char [(core/backward-char buf) kill-ring]
                               :next-line [(core/next-line buf) kill-ring]
                               :previous-line [(core/previous-line buf) kill-ring]
                               :move-beginning-of-line [(core/move-beginning-of-line buf) kill-ring]
                               :move-end-of-line [(core/move-end-of-line buf) kill-ring]
                               :kill-line (let [[new-buf killed] (kr/kill-line buf)]
                                            [new-buf (kr/kill-text kill-ring killed)])
                               :undo [(undo/undo buf) kill-ring]
                               :redo [(undo/redo buf) kill-ring]
                               :kill-region (if-let [mark (:mark buf)]
                                              (let [[new-buf killed] (kr/kill-region buf mark (:point buf))]
                                                [new-buf (kr/kill-text kill-ring killed)])
                                              [buf kill-ring])
                               :kill-ring-save (if-let [region (buffer/region-text buf)]
                                                 [buf (kr/kill-text kill-ring region)]
                                                 [buf kill-ring])
                               :yank [(kr/yank-text buf kill-ring) kill-ring]
                               :yank-pop (kr/yank-pop-text buf kill-ring)
                               [buf kill-ring])]
        (cond-> (state/assoc-current-buffer (assoc editor-state
                                                   :kill-ring new-kr
                                                   :key-sequence [])
                                            new-buf)
          (= command :quit) (assoc :running false))))))
