(ns ecro.window
  (:require
    [ecro.buffer :as b]
    [ecro.window-tree :as wt]))


(defn make-window
  "Create a new window with a buffer."
  ([buf]
   (make-window buf 80 24))
  ([buf width height]
   {:type :window
    :buffer buf
    :top 0
    :left 0
    :width width
    :height height
    :parent nil}))


(defn- make-container
  "Create a container window for splits."
  [direction children width height]
  {:type :container
   :direction direction
   :children children
   :width width
   :height height})


(defn make-frame
  "Create a new frame with a root window."
  ([root-window]
   (make-frame root-window 80 24))
  ([root-window width height]
   {:width width
    :height height
    :root-window root-window}))


(defn- update-window-positions
  "Update positions of all windows in a tree."
  [window top left]
  (if (= :window (:type window))
    (assoc window :top top :left left)
    (let [direction (:direction window)
          children (:children window)
          total-size (if (= :vertical direction) (:width window) (:height window))
          child-size (/ total-size (count children))
          updated-children (map-indexed
                             (fn [idx child]
                               (if (= :vertical direction)
                                 (update-window-positions child top (+ left (* idx child-size)))
                                 (update-window-positions child (+ top (* idx child-size)) left)))
                             children)]
      (assoc window :children updated-children))))


(defn split-window-vertical
  "Split a window vertically (side by side)."
  [frame window]
  (let [new-width (/ (:width window) 2)
        buf (b/make-buffer "*new*")
        new-window (assoc (make-window buf new-width (:height window)) :parent window)
        updated-window (assoc window :width new-width)
        container (make-container :vertical [updated-window new-window] (:width window) (:height window))]
    (if (= window (:root-window frame))
      (make-frame (update-window-positions container 0 0) (:width frame) (:height frame))
      frame)))


(defn split-window-horizontal
  "Split a window horizontally (stacked)."
  [frame window]
  (let [new-height (/ (:height window) 2)
        buf (b/make-buffer "*new*")
        new-window (assoc (make-window buf (:width window) new-height) :parent window)
        updated-window (assoc window :height new-height)
        container (make-container :horizontal [updated-window new-window] (:width window) (:height window))]
    (if (= window (:root-window frame))
      (make-frame (update-window-positions container 0 0) (:width frame) (:height frame))
      frame)))


(defn get-windows
  "Get all leaf windows in a frame."
  [frame]
  (wt/collect-windows (:root-window frame)))


(defn next-window
  "Get the next window in the frame."
  [frame window]
  (let [wins (get-windows frame)
        idx (.indexOf wins window)]
    (if (and (>= idx 0) (< (inc idx) (count wins)))
      (nth wins (inc idx))
      (first wins))))


(defn prev-window
  "Get the previous window in the frame."
  [frame window]
  (let [wins (get-windows frame)
        idx (.indexOf wins window)]
    (if (> idx 0)
      (nth wins (dec idx))
      (last wins))))


(defn delete-window
  "Remove a window from the frame tree. Returns updated frame."
  [frame window]
  (let [root (:root-window frame)
        new-root (wt/remove-window root window)]
    (if new-root
      (make-frame (update-window-positions new-root 0 0) (:width frame) (:height frame))
      (make-frame window (:width frame) (:height frame)))))


(defn delete-other-windows
  "Keep only the given window in the frame. Returns updated frame."
  [frame window]
  (make-frame (assoc window :parent nil :width (:width frame) :height (:height frame)) (:width frame) (:height frame)))


(defn other-window
  "Return the next window in the frame cycle."
  [frame window]
  (next-window frame window))


(defn resize-frame
  "Resize a frame and its root window."
  [frame width height]
  (let [root (:root-window frame)
        resized-root (assoc root :width width :height height)
        updated-root (update-window-positions resized-root 0 0)]
    (make-frame updated-root width height)))
