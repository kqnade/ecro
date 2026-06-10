(ns ecro.main
  (:require
    [ecro.buffer :as buffer]
    [ecro.core :as core]
    [ecro.file :as file]
    [ecro.keymap :as keymap]
    [ecro.kill-ring :as kr]
    [ecro.native :as native]
    [ecro.scroll :as scroll]
    [ecro.window :as window]))


(defonce lead-key (atom "ESC"))


(def shift-modifier 4)


(defn make-keymap
  "Create default keymap with lead-key."
  []
  (let [lk @lead-key]
    (-> (keymap/make-keymap)
        (keymap/define-key ["C-a"] :move-beginning-of-line)
        (keymap/define-key ["C-e"] :move-end-of-line)
        (keymap/define-key ["C-k"] :kill-line)
        (keymap/define-key ["C-z"] :undo)
        (keymap/define-key ["C-S-z"] :redo)
        (keymap/define-key ["C-/"] :undo)
        (keymap/define-key ["C-x"] :kill-region)
        (keymap/define-key ["C-c"] :kill-ring-save)
        (keymap/define-key ["C-v"] :yank)
        (keymap/define-key [lk "f"] :find-file)
        (keymap/define-key [lk "s"] :save-buffer)
        (keymap/define-key [lk "u"] :undo)
        (keymap/define-key [lk "q"] :quit)
        (keymap/define-key [lk lk] :keyboard-quit))))


(def default-keymap (make-keymap))


(defonce editor-state
  (atom {:running true
         :key-sequence []
         :keymap default-keymap
         :frame nil
         :current-buffer nil
         :message nil
         :kill-ring (kr/make-kill-ring)}))


;; Screen buffer for diff rendering
(defonce screen-buffer (atom []))


(defn expand-tabs
  "Expand tab characters to spaces."
  [line tab-width]
  (loop [chars (seq line)
         col 0
         result ""]
    (if (seq chars)
      (let [ch (first chars)]
        (if (= ch \tab)
          (let [spaces (- tab-width (mod col tab-width))]
            (recur (rest chars)
                   (+ col spaces)
                   (str result (apply str (repeat spaces " ")))))
          (recur (rest chars)
                 (inc col)
                 (str result ch))))
      result)))


(defn update-screen-line
  "Update a single line on screen, only outputting changes."
  [y old-line new-line width]
  (let [old (or old-line "")
        expanded (expand-tabs new-line 8)
        new (subs (format (str "%-" width "s") expanded) 0 width)]
    (when (not= old new)
      (print (str "\033[" (inc y) ";1H" new)))))


(defn screen-line
  "Return the exact rendered line stored in the diff buffer."
  [line width tab-width]
  (let [expanded (expand-tabs line tab-width)]
    (subs (format (str "%-" width "s") expanded) 0 width)))


(defn- shifted?
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


(defn- execute-command
  "Execute an editor command against state."
  [state command]
  (let [buf (:current-buffer state)
        kill-ring (or (:kill-ring state) (kr/make-kill-ring))
        [new-buf new-kr] (case command
                           :forward-char [(core/forward-char buf) kill-ring]
                           :backward-char [(core/backward-char buf) kill-ring]
                           :next-line [(core/next-line buf) kill-ring]
                           :previous-line [(core/previous-line buf) kill-ring]
                           :move-beginning-of-line [(core/move-beginning-of-line buf) kill-ring]
                           :move-end-of-line [(core/move-end-of-line buf) kill-ring]
                           :kill-line (let [[new-buf killed] (kr/kill-line buf)]
                                        [new-buf (kr/kill-text kill-ring killed)])
                           :undo [(buffer/undo buf) kill-ring]
                           :redo [(buffer/redo buf) kill-ring]
                           :kill-region (if-let [mark (:mark buf)]
                                          (let [[new-buf killed] (kr/kill-region buf mark (:point buf))]
                                            [new-buf (kr/kill-text kill-ring killed)])
                                          [buf kill-ring])
                           :kill-ring-save (if-let [region (buffer/region-text buf)]
                                             [buf (kr/kill-text kill-ring region)]
                                             [buf kill-ring])
                           :yank [(kr/yank-text buf kill-ring) kill-ring]
                           :find-file [(file/find-file "/tmp/ecro_test.txt") kill-ring]
                           :save-buffer [(file/save-buffer buf) kill-ring]
                           [buf kill-ring])]
    (cond-> (assoc state
                   :current-buffer new-buf
                   :kill-ring new-kr
                   :key-sequence [])
      (= command :quit) (assoc :running false))))


(defn render
  "Render editor state with diff updates."
  [state]
  (let [[width height] (or (native/get-terminal-size) [80 24])
        buf (:current-buffer state)
        tab-width (:tab-width buf 2)
        scroll-line (:scroll-line buf 0)
        lines (clojure.string/split (or (:text buf) "") #"\n" -1)
        visible-lines (take (- height 1) (drop scroll-line lines))
        old-screen @screen-buffer]
    ;; Hide cursor
    (print "\033[?25l")
    ;; Update only changed lines
    (doseq [[idx line] (map-indexed vector visible-lines)]
      (let [rendered (screen-line line width tab-width)]
        (when (not= (get old-screen idx) rendered)
          (print (str "\033[" (inc idx) ";1H" rendered)))))
    ;; Clear remaining lines if needed
    (doseq [idx (range (count visible-lines) (- height 1))]
      (update-screen-line idx (get old-screen idx) "" width))
    ;; Status line (always update)
    (let [name (or (:name buf) "*scratch*")
          modified (if (not= (:text buf) (:saved-text buf)) "*" "")
          key-seq (when (seq (:key-sequence state))
                    (str "C-" (clojure.string/join " " (:key-sequence state)) "-"))
          status (str " " name modified
                      (when key-seq (str "  " key-seq))
                      "    " (:message state))]
      (print (str "\033[" height ";1H\033[7m"
                  (format (str "%-" width "s") (or status ""))
                  "\033[0m")))
    ;; Position cursor (accounting for tab expansion)
    (let [point (:point buf 0)
          text (:text buf "")
          lines (clojure.string/split text #"\n" -1)
          [line-num _] (buffer/point-to-line-column buf point)
          line-text (nth lines line-num "")
          line-start (reduce + (map #(inc (count %)) (take line-num lines)))
          col-in-line (- point line-start)
          line-prefix (subs line-text 0 (max 0 (min col-in-line (count line-text))))
          visual-col (count (expand-tabs line-prefix tab-width))
          screen-row (- line-num scroll-line)]
      (print (str "\033[" (inc (max 0 screen-row)) ";" (inc visual-col) "H\033[?25h")))
    (flush)
    ;; Update screen buffer
    (reset! screen-buffer (mapv #(screen-line % width tab-width) visible-lines))))


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
    (update state :current-buffer buffer/insert-tab)

    ;; Escape - prefix key
    (= key-code 27)
    (let [new-seq (conj (:key-sequence state) "ESC")
          result (keymap/lookup-key (:keymap state) new-seq)]
      (cond
        (= result :prefix)
        (assoc state :key-sequence new-seq)

        (= result :keyboard-quit)
        (assoc state :key-sequence [])

        (nil? result)
        (if (seq (:key-sequence state))
          (assoc state :key-sequence [])
          state)

        :else
        (execute-command state result)))

    ;; Arrow keys
    (= key-code 1001)
    (let [buf (:current-buffer state)
          b (if (shifted? modifiers)
              (or (:mark buf) (buffer/set-mark buf))
              (buffer/deactivate-mark buf))]
      (assoc state :current-buffer (core/previous-line b)))

    (= key-code 1002)
    (let [buf (:current-buffer state)
          b (if (shifted? modifiers)
              (or (:mark buf) (buffer/set-mark buf))
              (buffer/deactivate-mark buf))]
      (assoc state :current-buffer (core/next-line b)))

    (= key-code 1003)
    (let [buf (:current-buffer state)
          b (if (shifted? modifiers)
              (or (:mark buf) (buffer/set-mark buf))
              (buffer/deactivate-mark buf))]
      (assoc state :current-buffer (core/backward-char b)))

    (= key-code 1004)
    (let [buf (:current-buffer state)
          b (if (shifted? modifiers)
              (or (:mark buf) (buffer/set-mark buf))
              (buffer/deactivate-mark buf))]
      (assoc state :current-buffer (core/forward-char b)))

    ;; Home
    (= key-code 1005)
    (update state :current-buffer core/move-beginning-of-line)

    ;; End
    (= key-code 1006)
    (update state :current-buffer core/move-end-of-line)

    ;; PageUp
    (= key-code 1007)
    (update state :current-buffer (fn [buf]
                                    (let [[_ h] (or (native/get-terminal-size) [80 24])]
                                      (scroll/scroll-up buf (dec h)))))

    ;; PageDown
    (= key-code 1008)
    (update state :current-buffer (fn [buf]
                                    (let [[_ h] (or (native/get-terminal-size) [80 24])]
                                      (scroll/scroll-down buf (dec h)))))

    ;; Insert
    (= key-code 1009)
    state

    ;; Delete (forward delete)
    (= key-code 1010)
    (update state :current-buffer buffer/delete-char-forward)

    ;; Function keys F1-F24
    (>= key-code 2000)
    state

    :else
    (let [key-str (key-name key-code modifiers)
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
        (execute-command state result)))))


(defn process-event
  "Process a terminal event."
  [state event]
  (let [next-state (if event
                     (case (:type event)
                       :key (handle-key state (:key_code event) (:modifiers event))
                       :resize (do (reset! screen-buffer []) state)
                       state)
                     state)
        [_ height] (or (native/get-terminal-size) [80 24])]
    (update next-state :current-buffer scroll/adjust-scroll-for-point (dec height))))


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
                       :kill-ring (kr/make-kill-ring)
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
