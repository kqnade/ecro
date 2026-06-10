(ns ecro.main
  (:require [ecro.native :as native]
            [ecro.buffer :as buffer]
            [ecro.core :as core]
            [ecro.keymap :as keymap]
            [ecro.window :as window]
            [ecro.file :as file]))

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

(def editor-state
  (atom {:running true
         :key-sequence []
         :keymap default-keymap
         :frame nil
         :current-buffer nil
         :message nil}))

(defn handle-key
  "Handle a key event and return updated state."
  [state key-code modifiers]
  (let [key-str (if (< key-code 32)
                  (str "C-" (char (+ key-code 96)))
                  (str (char key-code)))
        new-seq (conj (:key-sequence state) key-str)
        result (keymap/lookup-key (:keymap state) new-seq)]
    (cond
      (= result :prefix)
      (assoc state :key-sequence new-seq)
      
      (nil? result)
      (if (and (empty? (:key-sequence state)) (>= key-code 32) (< key-code 127))
        ;; Regular character input → insert into buffer
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
               :key-sequence [])))))

(defn render-line
  "Render a single line of text."
  [line y width]
  (let [padded (format (str "%-" width "s") line)]
    (print (str "\033[" (inc y) ";1H" (subs padded 0 (min width (count padded)))))))

(defn render-status-line
  "Render the status line at the bottom."
  [state height width]
  (let [buf (:current-buffer state)
        name (or (:name buf) "*scratch*")
        modified (if (not= (:text buf) (:saved-text buf)) "*" "")
        status (str " " name modified "    " (:message state))]
    (print (str "\033[" height ";1H\033[7m" (format (str "%-" width "s") status) "\033[0m"))))

(defn render
  "Render the editor state to the terminal."
  [state]
  (let [[width height] (or (native/get-terminal-size) [80 24])
        buf (:current-buffer state)
        lines (clojure.string/split (or (:text buf) "") #"\n" -1)
        visible-lines (take (- height 1) lines)]
    ;; Hide cursor before rendering
    (print "\033[?25l")
    ;; Move cursor to top-left instead of clearing screen
    (print "\033[H")
    ;; Render text
    (doseq [[idx line] (map-indexed vector visible-lines)]
      (render-line line idx width))
    ;; Fill remaining lines
    (doseq [idx (range (count visible-lines) (- height 1))]
      (render-line "" idx width))
    ;; Render status line
    (render-status-line state height width)
    ;; Position cursor and show
    (let [point (:point buf 0)
          [line col] (buffer/point-to-line-column buf point)]
      (print (str "\033[" (inc line) ";" (inc col) "H\033[?25h")))
    (flush)))

(defn log-event
  "Log event to file for debugging."
  [event]
  (spit "/tmp/ecro_debug.log"
        (str (java.time.LocalDateTime/now) " " event "\n")
        :append true))

(defn process-event
  "Process a terminal event."
  [state event]
  (log-event event)
  (if event
    (case (:type event)
      :key (handle-key state (:key_code event) (:modifiers event))
      :resize state
      state)
    state))

(defn -main
  "Main entry point for ecro editor."
  [& args]
  (try
    (native/init)
    (native/enable-raw-mode)
    (native/enter-alternate-screen)
    
    (swap! editor-state assoc
           :current-buffer (buffer/make-buffer "*scratch*")
           :frame (window/make-frame (window/make-window (buffer/make-buffer "*scratch*"))))
    
    (loop []
      (when (:running @editor-state)
        (render @editor-state)
        (let [event (native/poll-event)]
          (when event
            (swap! editor-state process-event event))
          (when (nil? event)
            (Thread/sleep 50))) ; sleep longer when no event
        (recur)))
    
    (finally
      (native/leave-alternate-screen)
      (native/disable-raw-mode)
      (native/shutdown))))
