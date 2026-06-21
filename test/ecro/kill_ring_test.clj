(ns ecro.kill-ring-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.kill-ring :as kr]
    [ecro.undo :as undo]))


(deftest test-make-kill-ring
  (testing "kill ring starts empty"
    (let [kr (kr/make-kill-ring)]
      (is (empty? (:entries kr)))
      (is (= 0 (:index kr))))))


(deftest test-kill-text
  (testing "adding text to kill ring"
    (let [kr (-> (kr/make-kill-ring)
                 (kr/kill-text "hello"))]
      (is (= ["hello"] (:entries kr)))
      (is (= 0 (:index kr))))))


(deftest test-kill-append
  (testing "appending to previous kill"
    (let [kr (-> (kr/make-kill-ring)
                 (kr/kill-text "hello")
                 (kr/kill-append " world"))]
      (is (= ["hello world"] (:entries kr))))))


(deftest test-yank
  (testing "yank returns last killed text"
    (let [kr (-> (kr/make-kill-ring)
                 (kr/kill-text "hello")
                 (kr/kill-text "world"))]
      (is (= "world" (kr/yank kr))))))


(deftest test-yank-pop
  (testing "yank-pop cycles through kill ring"
    (let [kr (-> (kr/make-kill-ring)
                 (kr/kill-text "first")
                 (kr/kill-text "second")
                 (kr/kill-text "third"))]
      (is (= "third" (kr/yank kr)))
      (let [kr2 (kr/yank-pop kr)]
        (is (= "second" (kr/yank kr2)))
        (let [kr3 (kr/yank-pop kr2)]
          (is (= "first" (kr/yank kr3)))
          (let [kr4 (kr/yank-pop kr3)]
            (is (= "third" (kr/yank kr4)))))))))


(deftest test-kill-line
  (testing "kill-line extracts text from point to end"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \e)
                  (b/insert-char \l)
                  (b/insert-char \l)
                  (b/insert-char \o)
                  (b/insert-char \space)
                  (b/insert-char \w)
                  (b/insert-char \o)
                  (b/insert-char \r)
                  (b/insert-char \l)
                  (b/insert-char \d)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward))
          [new-buf killed] (kr/kill-line buf)]
      (is (= "hello" (:text new-buf)))
      (is (= " world" killed)))))


(deftest test-kill-line-is-undoable
  (testing "kill-line can be undone"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello world")
                  (assoc :point 5))
          [new-buf _killed] (kr/kill-line buf)
          undone (undo/undo new-buf)]
      (is (= "hello" (:text new-buf)))
      (is (= "hello world" (:text undone)))
      (is (= 5 (:point undone))))))


(deftest test-kill-region
  (testing "kill-region extracts text between mark and point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \e)
                  (b/insert-char \l)
                  (b/insert-char \l)
                  (b/insert-char \o))
          ;; mark at 1, point at 4
          mark 1
          point 4
          [new-buf killed] (kr/kill-region buf mark point)]
      (is (= "ho" (:text new-buf)))
      (is (= "ell" killed)))))


(deftest test-kill-ring-max
  (testing "kill ring respects max size"
    (let [kr (-> (kr/make-kill-ring 2)
                 (kr/kill-text "one")
                 (kr/kill-text "two")
                 (kr/kill-text "three"))]
      (is (= 2 (count (:entries kr))))
      (is (= "three" (first (:entries kr))))
      (is (= "two" (second (:entries kr)))))))


(deftest test-kill-ring-save
  (testing "kill-ring-save adds region text without modifying buffer"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \e)
                  (b/insert-char \l)
                  (b/insert-char \l)
                  (b/insert-char \o)
                  (b/set-mark)
                  (b/move-point-backward))
          kr (-> (kr/make-kill-ring)
                 (kr/kill-ring-save buf))]
      (is (= "o" (b/region-text buf)))
      (is (= "o" (kr/yank kr)))
      (is (= "hello" (:text buf))))))


(deftest test-kill-ring-save-no-region
  (testing "kill-ring-save returns original kill ring when no region"
    (let [buf (b/make-buffer "test")
          kr (-> (kr/make-kill-ring)
                 (kr/kill-text "existing"))
          result (kr/kill-ring-save kr buf)]
      (is (= "existing" (kr/yank result))))))


(deftest test-yank-pop-text
  (testing "yank-pop replaces previous yank with next kill-ring entry"
    (let [kr (-> (kr/make-kill-ring)
                 (kr/kill-text "first")
                 (kr/kill-text "second"))
          buf (-> (b/make-buffer "test")
                  (kr/yank-text kr))
          [popped-buf popped-kr] (kr/yank-pop-text buf kr)]
      (is (= "first" (:text popped-buf)))
      (is (= 5 (:point popped-buf)))
      (is (= 1 (:index popped-kr)))))
  (testing "yank-pop does nothing when there was no previous yank"
    (let [buf (b/make-buffer "test")
          kr (kr/make-kill-ring)
          [new-buf new-kr] (kr/yank-pop-text buf kr)]
      (is (= "" (:text new-buf)))
      (is (= new-kr kr)))))


(deftest test-yank-text
  (testing "yank-text inserts kill ring entry at point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \i))
          kr (-> (kr/make-kill-ring)
                 (kr/kill-text " there"))
          new-buf (kr/yank-text buf kr)]
      (is (= "hi there" (:text new-buf)))
      (is (= 8 (:point new-buf))))))
