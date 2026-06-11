(ns ecro.key-test
  (:require
    [clojure.test :refer :all]
    [ecro.bindings :as bindings]
    [ecro.buffer :as b]
    [ecro.key :as key]))


(deftest test-key-name-control-shift-and-control-slash
  (testing "terminal key codes map to keymap names"
    (is (= "C-z" (key/key-name 26 1)))
    (is (= "C-S-z" (key/key-name 26 5)))
    (is (= "C-/" (key/key-name 31 1)))))


(deftest test-handle-key-esc-prefix
  (testing "ESC starts prefix sequence"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          new-state (key/handle-key state 27 0)] ; ESC
      (is (= ["ESC"] (:key-sequence new-state))))))


(deftest test-handle-key-esc-prefix-complete
  (testing "ESC f activates find-file minibuffer"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence ["ESC"]}
          new-state (key/handle-key state 102 0)] ; f after ESC
      (is (= [] (:key-sequence new-state)))
      (is (some? (:minibuffer new-state)))
      (is (= "Find file: " (get-in new-state [:minibuffer :prompt]))))))


(deftest test-repeated-shift-arrow-keeps-selection-buffer
  (testing "repeated Shift+Right extends selection without replacing buffer with mark"
    (let [state {:current-buffer (assoc (b/make-buffer "test") :text "abc")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          state' (key/handle-key state 1004 key/shift-modifier)
          state'' (key/handle-key state' 1004 key/shift-modifier)
          buf (:current-buffer state'')]
      (is (= 0 (:mark buf)))
      (is (= 2 (:point buf)))
      (is (= "abc" (:text buf))))))
