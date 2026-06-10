(ns ecro.main-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.main :as main]))


(deftest test-handle-key-forward-char
  (testing "C-f moves point forward"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \a))
                 :keymap main/default-keymap
                 :key-sequence []}
          new-state (main/handle-key state 6 1)] ; Ctrl-f
      (is (= 1 (:point (:current-buffer new-state)))))))


(deftest test-handle-key-backward-char
  (testing "C-b moves point backward"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \a)
                                     (b/insert-char \b))
                 :keymap main/default-keymap
                 :key-sequence []}
          new-state (main/handle-key state 2 1)] ; Ctrl-b
      (is (= 1 (:point (:current-buffer new-state)))))))


(deftest test-handle-key-prefix
  (testing "C-x starts prefix sequence"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap main/default-keymap
                 :key-sequence []}
          new-state (main/handle-key state 24 1)] ; Ctrl-x
      (is (= ["C-x"] (:key-sequence new-state))))))


(deftest test-handle-key-prefix-complete
  (testing "C-x C-f completes find-file command"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap main/default-keymap
                 :key-sequence ["C-x"]}
          new-state (main/handle-key state 6 1)] ; Ctrl-f after C-x
      (is (= [] (:key-sequence new-state)))
      (is (not= "test" (:name (:current-buffer new-state)))))))


(deftest test-editor-state-initialized
  (testing "editor state has required keys"
    (let [state @main/editor-state]
      (is (contains? state :running))
      (is (contains? state :key-sequence))
      (is (contains? state :keymap))
      (is (contains? state :current-buffer)))))
