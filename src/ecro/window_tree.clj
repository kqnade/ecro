(ns ecro.window-tree)


(defn collect-windows
  "Collect all leaf windows from a window tree."
  [window]
  (if (= :window (:type window))
    [window]
    (mapcat collect-windows (:children window))))
