(ns ecro.bindings
  (:require
    [ecro.keymap :as keymap]))


(defonce lead-key (atom "ESC"))


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
