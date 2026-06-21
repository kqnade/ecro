(ns ecro.bindings-test
  (:require
    [clojure.test :refer :all]
    [ecro.bindings :as bindings]
    [ecro.keymap :as keymap]))


(deftest test-default-keymap-bindings
  (testing "default keymap includes movement and editing commands"
    (let [km bindings/default-keymap]
      (is (= :move-beginning-of-line (keymap/lookup-key km ["C-a"])))
      (is (= :move-end-of-line (keymap/lookup-key km ["C-e"])))
      (is (= :kill-line (keymap/lookup-key km ["C-k"])))
      (is (= :undo (keymap/lookup-key km ["C-z"])))
      (is (= :redo (keymap/lookup-key km ["C-S-z"])))
      (is (= :yank (keymap/lookup-key km ["C-v"])))
      (is (= :set-mark-command (keymap/lookup-key km ["C-SPC"])))
      (is (= :forward-word (keymap/lookup-key km ["M-f"])))
      (is (= :backward-word (keymap/lookup-key km ["M-b"])))
      (is (= :beginning-of-buffer (keymap/lookup-key km ["M-<"])))
      (is (= :end-of-buffer (keymap/lookup-key km ["M->"])))
      (is (= :scroll-down-command (keymap/lookup-key km ["M-v"])))
      (is (= :yank-pop (keymap/lookup-key km ["M-y"])))))
  (testing "default keymap includes lead-key prefix commands"
    (let [km bindings/default-keymap]
      (is (= :find-file (keymap/lookup-key km ["ESC" "f"])))
      (is (= :save-buffer (keymap/lookup-key km ["ESC" "s"])))
      (is (= :delete-window (keymap/lookup-key km ["ESC" "0"])))
      (is (= :delete-other-windows (keymap/lookup-key km ["ESC" "1"])))
      (is (= :other-window (keymap/lookup-key km ["ESC" "o"])))
      (is (= :write-file (keymap/lookup-key km ["ESC" "w"])))
      (is (= :switch-to-buffer (keymap/lookup-key km ["ESC" "b"])))
      (is (= :kill-buffer (keymap/lookup-key km ["ESC" "k"])))
      (is (= :list-buffers (keymap/lookup-key km ["ESC" "B"])))
      (is (= :keyboard-quit (keymap/lookup-key km ["ESC" "ESC"]))))))
