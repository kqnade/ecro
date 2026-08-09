(ns ecro.render
  (:require
    [clojure.string :as str]
    [ecro.buffer :as buffer]
    [ecro.native :as native]
    [ecro.notification :as notification]
    [ecro.skk.ui :as skk-ui]))


(defonce screen-buffer (atom []))


(defn- terminal-width
  [text]
  (or (native/text-width text)
      (throw (IllegalStateException. "ecro_core is required to measure terminal text"))))


(defn- terminal-prefix-length
  [text width]
  (or (native/text-prefix-utf16-length-for-width text width)
      (throw (IllegalStateException. "ecro_core is required to truncate terminal text"))))


(defn- ansi-csi-end
  [^String text offset]
  (when (and (< (inc offset) (count text))
             (= 0x1b (int (.charAt text offset)))
             (= \[ (.charAt text (inc offset))))
    (loop [index (+ offset 2)]
      (when (< index (count text))
        (let [ch (int (.charAt text index))]
          (if (<= 0x40 ch 0x7e)
            (inc index)
            (recur (inc index))))))))


(defn- sgr-active-after
  [^String text offset end active?]
  (if (= \m (.charAt text (dec end)))
    (let [sequence (subs text offset end)]
      (not (or (= sequence "\033[m")
               (= sequence "\033[0m"))))
    active?))


(defn- plain-segment-end
  [^String text offset stop-at-tab?]
  (loop [index offset]
    (if (or (>= index (count text))
            (ansi-csi-end text index)
            (and stop-at-tab? (= \tab (.charAt text index))))
      index
      (recur (inc index)))))


(defn display-width
  "Return the number of terminal cells occupied by text."
  [^String text]
  (loop [offset 0
         width 0]
    (if (< offset (count text))
      (if-let [ansi-end (ansi-csi-end text offset)]
        (recur ansi-end width)
        (let [segment-end (plain-segment-end text offset false)
              segment (subs text offset segment-end)]
          (recur segment-end (+ width (terminal-width segment)))))
      width)))


(defn- truncate-to-width
  [^String text width]
  (let [result (StringBuilder.)]
    (loop [offset 0
           current-width 0
           sgr-active? false]
      (if (< offset (count text))
        (if-let [ansi-end (ansi-csi-end text offset)]
          (do
            (.append result (subs text offset ansi-end))
            (recur ansi-end
                   current-width
                   (sgr-active-after text offset ansi-end sgr-active?)))
          (let [segment-end (plain-segment-end text offset false)
                segment (subs text offset segment-end)
                segment-width (terminal-width segment)
                next-width (+ current-width segment-width)]
            (if (<= next-width width)
              (do
                (.append result segment)
                (recur segment-end next-width sgr-active?))
              (do
                (let [prefix-length (terminal-prefix-length segment (- width current-width))]
                  (.append result (subs segment 0 prefix-length)))
                (when sgr-active?
                  (.append result "\033[0m"))
                (str result)))))
        (str result)))))


(defn- fit-to-width
  [text width]
  (let [truncated (truncate-to-width text width)
        padding (- width (display-width truncated))]
    (str truncated (apply str (repeat padding " ")))))


(defn reset-screen-buffer!
  "Force the next render to redraw all lines."
  []
  (reset! screen-buffer []))


(defn expand-tabs
  "Expand tab characters to spaces."
  [^String line tab-width]
  (let [result (StringBuilder.)]
    (loop [offset 0
           col 0]
      (if (< offset (count line))
        (if-let [ansi-end (ansi-csi-end line offset)]
          (do
            (.append result (subs line offset ansi-end))
            (recur ansi-end col))
          (if (= \tab (.charAt line offset))
            (let [spaces (- tab-width (mod col tab-width))]
              (.append result (apply str (repeat spaces " ")))
              (recur (inc offset) (+ col spaces)))
            (let [segment-end (plain-segment-end line offset true)
                  segment (subs line offset segment-end)]
              (.append result segment)
              (recur segment-end (+ col (terminal-width segment))))))
        (str result)))))


(defn update-screen-line
  "Update a single line on screen, only outputting changes."
  [y old-line new-line width]
  (let [old (or old-line "")
        expanded (expand-tabs new-line 8)
        new (fit-to-width expanded width)]
    (when (not= old new)
      (print (str "\033[" (inc y) ";1H" new)))))


(defn screen-line
  "Return the exact rendered line stored in the diff buffer."
  [line width tab-width]
  (let [expanded (expand-tabs line tab-width)]
    (fit-to-width expanded width)))


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
                      (skk-ui/status-message (:current-buffer state))
                      (:message state))))))


(defn- region-range
  "Return [start end] of active region, or nil."
  [buf]
  (when (:mark buf)
    [(min (:mark buf) (:point buf))
     (max (:mark buf) (:point buf))]))


(defn- line-start-offset
  "Calculate the buffer offset at the start of a visible line."
  [all-lines scroll-line line-idx]
  (+ (reduce + (map #(inc (count %)) (take scroll-line all-lines)))
     (reduce + (map #(inc (count %)) (take line-idx (drop scroll-line all-lines))))))


(defn- render-line-with-region
  "Render a single line, highlighting the active region with reverse video."
  [line line-start region width tab-width]
  (if (and region (< line-start (second region)) (>= (+ line-start (count line)) (first region)))
    (let [rel-start (max 0 (- (first region) line-start))
          rel-end (min (count line) (- (second region) line-start))
          before (subs line 0 rel-start)
          inside (subs line rel-start rel-end)
          after (subs line rel-end)
          rendered (str before "\033[7m" inside "\033[0m" after)]
      (screen-line rendered width tab-width))
    (screen-line line width tab-width)))


(defn- rendered-visible-lines
  "Render visible buffer lines exactly as they should be stored in screen-buffer."
  [lines visible-lines scroll-line region width tab-width]
  (mapv (fn [idx line]
          (let [line-start (line-start-offset lines scroll-line idx)]
            (render-line-with-region line line-start region width tab-width)))
        (range)
        visible-lines))


(defn render
  "Render editor state with diff updates."
  [state]
  (let [[width height] (or (native/get-terminal-size) [80 24])
        buf (:current-buffer state)
        tab-width (:tab-width buf 2)
        scroll-line (:scroll-line buf 0)
        lines (str/split (or (:text buf) "") #"\n" -1)
        visible-lines (take (- height 1) (drop scroll-line lines))
        region (region-range buf)
        rendered-lines (rendered-visible-lines lines visible-lines scroll-line region width tab-width)
        old-screen @screen-buffer]
    (print "\033[?25l")
    (doseq [[idx rendered] (map-indexed vector rendered-lines)]
      (when (not= (get old-screen idx) rendered)
        (print (str "\033[" (inc idx) ";1H" rendered))))
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
          visual-col (display-width (expand-tabs line-prefix tab-width))
          screen-row (- line-num scroll-line)]
      (print (str "\033[" (inc (max 0 screen-row)) ";" (inc visual-col) "H\033[?25h")))
    (flush)
    (reset! screen-buffer rendered-lines)))
