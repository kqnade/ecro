(ns ecro.window-tree)


(defn collect-windows
  "Collect all leaf windows from a window tree."
  [window]
  (if (= :window (:type window))
    [window]
    (mapcat collect-windows (:children window))))


(defn update-window
  "Update the leaf with the given ID in a window tree."
  [tree window-id f]
  (if (= :window (:type tree))
    (if (= window-id (:id tree))
      (f tree)
      tree)
    (update tree :children #(mapv (fn [child]
                                    (update-window child window-id f))
                                  %))))


(defn remove-window
  "Remove a leaf window from a window tree. Returns the remaining tree,
   or nil if the last window is removed."
  [tree window]
  (if (= :window (:type tree))
    (when (not= tree window) tree)
    (let [new-children (keep #(remove-window % window) (:children tree))]
      (cond
        (empty? new-children) nil
        (= 1 (count new-children)) (first new-children)
        :else (assoc tree :children (vec new-children))))))
