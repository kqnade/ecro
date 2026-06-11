(ns ecro.state-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.state :as state]))


(deftest test-add-buffer-to-list
  (testing "adding a buffer appends to buffer list"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf1
                 :buffers [buf1]}
          new-state (state/add-buffer state buf2)]
      (is (= 2 (count (:buffers new-state))))
      (is (= "test.txt" (:name (last (:buffers new-state))))))))


(deftest test-switch-to-existing-buffer
  (testing "switch-to-buffer changes current buffer to existing one"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf1
                 :buffers [buf1 buf2]}
          new-state (state/switch-to-buffer state "test.txt")]
      (is (= "test.txt" (:name (:current-buffer new-state)))))))


(deftest test-switch-to-buffer-creates-new
  (testing "switch-to-buffer creates new buffer if name not found"
    (let [buf1 (b/make-buffer "*scratch*")
          state {:current-buffer buf1
                 :buffers [buf1]}
          new-state (state/switch-to-buffer state "new.txt")]
      (is (= "new.txt" (:name (:current-buffer new-state))))
      (is (= 2 (count (:buffers new-state)))))))


(deftest test-kill-buffer
  (testing "kill-buffer removes buffer from list and switches to another"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf2
                 :buffers [buf1 buf2]}
          new-state (state/kill-buffer state "test.txt")]
      (is (= 1 (count (:buffers new-state))))
      (is (= "*scratch*" (:name (:current-buffer new-state)))))))


(deftest test-kill-buffer-keeps-last
  (testing "kill-buffer keeps last buffer and shows message"
    (let [buf1 (b/make-buffer "*scratch*")
          state {:current-buffer buf1
                 :buffers [buf1]}
          new-state (state/kill-buffer state "*scratch*")]
      (is (= 1 (count (:buffers new-state))))
      (is (= "Can't kill last buffer" (:message new-state))))))


(deftest test-get-buffer-names
  (testing "get-buffer-names returns list of buffer names"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:buffers [buf1 buf2]}]
      (is (= ["*scratch*" "test.txt"] (state/get-buffer-names state))))))


(deftest test-current-buffer-edits-update-buffer-list
  (testing "editing current buffer keeps buffer list synchronized"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "other.txt")
          state {:current-buffer buf1
                 :buffers [buf1 buf2]}
          edited (state/assoc-current-buffer state (b/insert-char buf1 \a))
          switched-away (state/switch-to-buffer edited "other.txt")
          switched-back (state/switch-to-buffer switched-away "*scratch*")]
      (is (= "a" (:text (:current-buffer edited))))
      (is (= "a" (:text (first (:buffers edited)))))
      (is (= "a" (:text (:current-buffer switched-back)))))))
