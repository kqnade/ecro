(ns ecro.key
  (:require
    [ecro.buffer :as buffer]
    [ecro.command :as command]
    [ecro.core :as core]
    [ecro.file :as file]
    [ecro.keymap :as keymap]
    [ecro.native :as native]
    [ecro.render :as render]
    [ecro.scroll :as scroll]
    [ecro.state :as state]))


(def shift-modifier 4)


(defn shifted?
  [modifiers]
  (pos? (bit-and modifiers shift-modifier)))


(defn key-name
  "Return the keymap name for a terminal key code and modifier bitset."
  [key-code modifiers]
  (cond
    (= key-code 31) "C-/"

    (and (= key-code 26) (shifted? modifiers)) "C-S-z"

    (< key-code 32) (str "C-" (char (+ key-code 96)))

    :else (str (char key-code))))


(defn- handle-prefix-result
  [editor-state new-seq result]
  (cond
    (= result :prefix)
    (assoc editor-state :key-sequence new-seq)

    (= result :keyboard-quit)
    (assoc editor-state :key-sequence [])

    (nil? result)
    (if (seq (:key-sequence editor-state))
      (assoc editor-state :key-sequence [])
      editor-state)

    :else
    (command/execute-command editor-state result)))


(defn- prepare-selection
  [buf modifiers]
  (if (shifted? modifiers)
    (if (:mark buf)
      buf
      (buffer/set-mark buf))
    (buffer/deactivate-mark buf)))


(defn- handle-minibuffer-key
  "Handle a key event when minibuffer is active."
  [state key-code]
  (let [mb (:minibuffer state)]
    (cond
      (= key-code 13)  ; Enter - complete
      (let [input (:text (:buffer mb))
            cmd (:command mb)]
        (case cmd
          :open-file (let [new-buf (file/find-file input)]
                       (-> (assoc state :minibuffer nil)
                           (state/assoc-current-buffer new-buf)))
          (assoc state :minibuffer nil)))

      (= key-code 27)  ; Escape - cancel
      (assoc state :minibuffer nil)

      (= key-code 127) ; Backspace
      (update-in state [:minibuffer :buffer] buffer/delete-char-backward)

      (>= key-code 32) ; Printable char
      (update-in state [:minibuffer :buffer] buffer/insert-char (char key-code))

      :else state)))


(defn handle-key
  "Handle a key event and return updated state."
  [editor-state key-code modifiers]
  (if (:minibuffer editor-state)
    (handle-minibuffer-key editor-state key-code)
    (cond
      (= key-code 127)
      (state/assoc-current-buffer editor-state (buffer/delete-char-backward (:current-buffer editor-state)))

      (= key-code 13)
      (state/assoc-current-buffer editor-state (buffer/insert-newline (:current-buffer editor-state)))

      (= key-code 9)
      (state/assoc-current-buffer editor-state (buffer/insert-tab (:current-buffer editor-state)))

      (= key-code 27)
      (let [new-seq (conj (:key-sequence editor-state) "ESC")
            result (keymap/lookup-key (:keymap editor-state) new-seq)]
        (handle-prefix-result editor-state new-seq result))

      (= key-code 1001)
      (state/assoc-current-buffer editor-state
                                  (core/previous-line (prepare-selection (:current-buffer editor-state) modifiers)))

      (= key-code 1002)
      (state/assoc-current-buffer editor-state
                                  (core/next-line (prepare-selection (:current-buffer editor-state) modifiers)))

      (= key-code 1003)
      (state/assoc-current-buffer editor-state
                                  (core/backward-char (prepare-selection (:current-buffer editor-state) modifiers)))

      (= key-code 1004)
      (state/assoc-current-buffer editor-state
                                  (core/forward-char (prepare-selection (:current-buffer editor-state) modifiers)))

      (= key-code 1005)
      (state/assoc-current-buffer editor-state (core/move-beginning-of-line (:current-buffer editor-state)))

      (= key-code 1006)
      (state/assoc-current-buffer editor-state (core/move-end-of-line (:current-buffer editor-state)))

      (= key-code 1007)
      (let [[_ h] (or (native/get-terminal-size) [80 24])]
        (state/assoc-current-buffer editor-state (scroll/scroll-up (:current-buffer editor-state) (dec h))))

      (= key-code 1008)
      (let [[_ h] (or (native/get-terminal-size) [80 24])]
        (state/assoc-current-buffer editor-state (scroll/scroll-down (:current-buffer editor-state) (dec h))))

      (= key-code 1009)
      editor-state

      (= key-code 1010)
      (state/assoc-current-buffer editor-state (buffer/delete-char-forward (:current-buffer editor-state)))

      (>= key-code 2000)
      editor-state

      :else
      (let [key-str (key-name key-code modifiers)
            new-seq (conj (:key-sequence editor-state) key-str)
            result (keymap/lookup-key (:keymap editor-state) new-seq)]
        (cond
          (= result :prefix)
          (assoc editor-state :key-sequence new-seq)

          (nil? result)
          (if (and (empty? (:key-sequence editor-state))
                   (>= key-code 32)
                   (< key-code 127))
            (state/assoc-current-buffer editor-state (buffer/insert-char (:current-buffer editor-state) (char key-code)))
            (assoc editor-state :key-sequence []))

          :else
          (command/execute-command editor-state result))))))


(defn process-event
  "Process a terminal event."
  [editor-state event]
  (let [next-state (if event
                     (case (:type event)
                       :key (handle-key editor-state (:key_code event) (:modifiers event))
                       :resize (do (render/reset-screen-buffer!) editor-state)
                       editor-state)
                     editor-state)
        [_ height] (or (native/get-terminal-size) [80 24])]
    (update next-state :current-buffer scroll/adjust-scroll-for-point (dec height))))
