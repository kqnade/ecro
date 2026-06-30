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
        (keymap/define-key ["C-SPC"] :set-mark-command)
        (keymap/define-key ["M-f"] :forward-word)
        (keymap/define-key ["M-b"] :backward-word)
        (keymap/define-key ["M-<"] :beginning-of-buffer)
        (keymap/define-key ["M->"] :end-of-buffer)
        (keymap/define-key ["M-v"] :scroll-down-command)
        (keymap/define-key ["M-y"] :yank-pop)
        (keymap/define-key [lk "f"] :find-file)
        (keymap/define-key [lk "s"] :save-buffer)
        (keymap/define-key [lk "u"] :undo)
        (keymap/define-key [lk "q"] :quit)
        (keymap/define-key [lk "0"] :delete-window)
        (keymap/define-key [lk "1"] :delete-other-windows)
        (keymap/define-key [lk "o"] :other-window)
        (keymap/define-key [lk "w"] :write-file)
        (keymap/define-key [lk "b"] :switch-to-buffer)
        (keymap/define-key [lk "k"] :kill-buffer)
        (keymap/define-key [lk "B"] :list-buffers)
        (keymap/define-key [lk "n"] :toggle-skk)
        (keymap/define-key [lk lk] :keyboard-quit))))


(def default-keymap (make-keymap))
