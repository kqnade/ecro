(ns ecro.main
  (:require
    [ecro.buffer :as buffer]
    [ecro.core :as core]
    [ecro.file :as file]
    [ecro.keymap :as keymap]
    [ecro.native :as native]
    [ecro.window :as window]))


(def default-keymap
  (-> (keymap/make-keymap)
      (keymap/define-key ["C-f"] :forward-char)
      (keymap/define-key ["C-b"] :backward-char)
      (keymap/define-key ["C-n"] :next-line)
      (keymap/define-key ["C-p"] :previous-line)
      (keymap/define-key ["C-a"] :move-beginning-of-line)
      (keymap/define-key ["C-e"] :move-end-of-line)
      (keymap/define-key ["C-k"] :kill-line)
      (keymap/define-key ["C-x" "C-f"] :find-file)
      (keymap/define-key ["C-x" "C-s"] :save-buffer)))


(defonce editor-state
  (atom {:running true
         :key-sequence []
         :keymap default-keymap
         :frame nil
         :current-buffer nil
         :message nil}))


;; Screen buffer for diff rendering
(defonce screen-buffer (atom []))


(defn update-screen-line
  "Update a single line on screen, only outputting changes."
  [y old-line new-line width]
  (let [old (or old-line "")
        new (subs (format (str "%-" width "s") new-line) 0 width)]
    (when (not= old new)
      (print (str "\033[" (inc y) ";1H" new)))))


(defn render
  "Render editor state with diff updates."
  [state]
  (let [[width height] (or (native/get-terminal-size) [80 24])
        buf (:current-buffer state)
        lines (clojure.string/split (or (:text buf) "") #"\n" -1)
        visible-lines (take (- height 1) lines)
        old-screen @screen-buffer]
    ;; Hide cursor
    (print "\033[?25l")
    ;; Update only changed lines
    (doseq [[idx line] (map-indexed vector visible-lines)]
      (update-screen-line idx (get old-screen idx) line width))
    ;; Clear remaining lines if needed
    (doseq [idx (range (count visible-lines) (- height 1))]
      (update-screen-line idx (get old-screen idx) "" width))
    ;; Status line (always update)
    (let [name (or (:name buf) "*scratch*")
          modified (if (not= (:text buf) (:saved-text buf)) "*" "")
          status (str " " name modified "    " (:message state))]
      (print (str "\033[" height ";1H\033[7m"
                  (format (str "%-" width "s") status)
                  "\033[0m")))
    ;; Position cursor
    (let [point (:point buf 0)
          [line col] (buffer/point-to-line-column buf point)]
      (print (str "\033[" (inc line) ";" (inc col) "H\033[?25h")))
    (flush)
    ;; Update screen buffer
    (reset! screen-buffer (mapv #(format (str "%-" width "s") %) visible-lines))))


(defn handle-key
  "Handle a key event and return updated state."
  [state key-code modifiers]
  (cond
    ;; Backspace (DEL)
    (= key-code 127)
    (update state :current-buffer buffer/delete-char-backward)

    ;; Enter (RET) - insert newline
    (= key-code 13)
    (update state :current-buffer buffer/insert-newline)

    ;; Tab
    (= key-code 9)
    (update state :current-buffer buffer/insert-char \tab)

    ;; Escape - cancel prefix
    (= key-code 27)
    (assoc state :key-sequence [])

    ;; Home
    (= key-code 1005)
    (update state :current-buffer core/move-beginning-of-line)

    ;; End
    (= key-code 1006)
    (update state :current-buffer core/move-end-of-line)

    ;; PageUp
    (= key-code 1007)
    state

    ;; PageDown
    (= key-code 1008)
    state

    ;; Insert
    (= key-code 1009)
    state

    ;; Delete (forward delete)
    (= key-code 1010)
    (update state :current-buffer buffer/delete-char-forward)

    ;; Function keys F1-F24
    (>= key-code 2000)
    state

    ;; Ctrl-C (3) - quit
    (= key-code 3)
    (assoc state :running false)

    :else
    (let [key-str (if (< key-code 32)
                    (str "C-" (char (+ key-code 96)))
                    (str (char key-code)))
          new-seq (conj (:key-sequence state) key-str)
          result (keymap/lookup-key (:keymap state) new-seq)]
      (cond
        (= result :prefix)
        (assoc state :key-sequence new-seq)

        (nil? result)
        (if (and (empty? (:key-sequence state))
                 (>= key-code 32)
                 (< key-code 127))
          ;; Regular character input
          (update state :current-buffer buffer/insert-char (char key-code))
          (assoc state :key-sequence []))

        :else
        (let [buf (:current-buffer state)
              new-buf (case result
                        :forward-char (core/forward-char buf)
                        :backward-char (core/backward-char buf)
                        :next-line (core/next-line buf)
                        :previous-line (core/previous-line buf)
                        :move-beginning-of-line (core/move-beginning-of-line buf)
                        :move-end-of-line (core/move-end-of-line buf)
                        :kill-line (core/kill-line buf)
                        :find-file (file/find-file "/tmp/ecro_test.txt")
                        :save-buffer (file/save-buffer buf)
                        buf)]
          (assoc state
                 :current-buffer new-buf
                 :key-sequence []))))))


(defn process-event
  "Process a terminal event."
  [state event]
  (if event
    (case (:type event)
      :key (handle-key state (:key_code event) (:modifiers event))
      :resize (do (reset! screen-buffer []) state)
      state)
    state))


(defn -main
  "Main entry point for ecro editor."
  [& args]
  (try
    (native/init)
    (native/enable-raw-mode)
    (native/enter-alternate-screen)

    (let [state (atom {:running true
                       :key-sequence []
                       :keymap default-keymap
                       :frame nil
                       :current-buffer (buffer/make-buffer "*scratch*")
                       :message nil})]
      ;; Initial render
      (render @state)

      ;; Main loop with blocking reads
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
