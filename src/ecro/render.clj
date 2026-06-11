(ns ecro.render
  (:require
    [clojure.string :as str]
    [ecro.buffer :as buffer]
    [ecro.native :as native]
    [ecro.notification :as notification]))


(defonce screen-buffer (atom []))


(defn reset-screen-buffer!
  "Force the next render to redraw all lines."
  []
  (reset! screen-buffer []))


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


(defn status-line
  "Build the status line string from editor state."
  [state]
  (if-let [mb (:minibuffer state)]
    (str (:prompt mb) (:text (:buffer mb)))
    (let [buf (:current-buffer state)
          name (or (:name buf) "*scratch*")
          modified (if (not= (:text buf) (:saved-text buf)) "*" "")
          key-seq (when (seq (:key-sequence state))
                    (str (str/join " " (:key-sequence state)) " "))]
      (str " " name modified
           (when key-seq (str "  " key-seq))
           "    " (or (notification/text (:notification state))
                      (:message state))))))


(defn render
  "Render editor state with diff updates."
  [state]
  (let [[width height] (or (native/get-terminal-size) [80 24])
        buf (:current-buffer state)
        tab-width (:tab-width buf 2)
        scroll-line (:scroll-line buf 0)
        lines (str/split (or (:text buf) "") #"\n" -1)
        visible-lines (take (- height 1) (drop scroll-line lines))
        old-screen @screen-buffer]
    (print "\033[?25l")
    (doseq [[idx line] (map-indexed vector visible-lines)]
      (let [rendered (screen-line line width tab-width)]
        (when (not= (get old-screen idx) rendered)
          (print (str "\033[" (inc idx) ";1H" rendered)))))
    (doseq [idx (range (count visible-lines) (- height 1))]
      (update-screen-line idx (get old-screen idx) "" width))
    (let [status (status-line state)
          status-line-rendered (screen-line (or status "") width 1)]
      (print (str "\033[" height ";1H\033[7m"
                  status-line-rendered
                  "\033[0m")))
    (let [point (:point buf 0)
          text (:text buf "")
          lines (str/split text #"\n" -1)
          [line-num _] (buffer/point-to-line-column buf point)
          line-text (nth lines line-num "")
          line-start (reduce + (map #(inc (count %)) (take line-num lines)))
          col-in-line (- point line-start)
          line-prefix (subs line-text 0 (max 0 (min col-in-line (count line-text))))
          visual-col (count (expand-tabs line-prefix tab-width))
          screen-row (- line-num scroll-line)]
      (print (str "\033[" (inc (max 0 screen-row)) ";" (inc visual-col) "H\033[?25h")))
    (flush)
    (reset! screen-buffer (mapv #(screen-line % width tab-width) visible-lines))))
