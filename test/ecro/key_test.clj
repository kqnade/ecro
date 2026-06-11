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
  (testing "ESC f completes find-file command"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence ["ESC"]}
          new-state (key/handle-key state 102 0)] ; f after ESC
      (is (= [] (:key-sequence new-state)))
      (is (not= "test" (:name (:current-buffer new-state)))))))
