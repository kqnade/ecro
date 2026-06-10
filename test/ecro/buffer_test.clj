(ns ecro.buffer-test
  (:require [clojure.test :refer :all]
            [ecro.buffer :as b]))

(deftest test-create-buffer
  (testing "buffer creation with name"
    (let [buf (b/make-buffer "*test*")]
      (is (= "*test*" (:name buf)))
      (is (= "" (:text buf)))
      (is (= 0 (:point buf))))))

(deftest test-insert-char
  (testing "inserting a single character"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a))]
      (is (= "a" (:text buf)))
      (is (= 1 (:point buf))))))

(deftest test-insert-multiple-chars
  (testing "inserting multiple characters sequentially"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \e)
                  (b/insert-char \l)
                  (b/insert-char \l)
                  (b/insert-char \o))]
      (is (= "hello" (:text buf)))
      (is (= 5 (:point buf))))))

(deftest test-delete-char-forward
  (testing "deleting a character at point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (b/insert-char \c)
                  (b/move-point-backward)
                  (b/delete-char-forward))]
      (is (= "ab" (:text buf)))
      (is (= 2 (:point buf))))))

(deftest test-delete-char-forward-at-end
  (testing "deleting at end of buffer does nothing"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/delete-char-forward))]
      (is (= "a" (:text buf)))
      (is (= 1 (:point buf))))))

(deftest test-move-point-forward
  (testing "moving point forward"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \i)
                  (b/move-point-backward)
                  (b/move-point-forward))]
      (is (= 2 (:point buf))))))

(deftest test-move-point-backward
  (testing "moving point backward"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \i)
                  (b/move-point-backward))]
      (is (= 1 (:point buf))))))

(deftest test-move-point-backward-at-beginning
  (testing "moving backward at beginning stays at 0"
    (let [buf (-> (b/make-buffer "test")
                  (b/move-point-backward))]
      (is (= 0 (:point buf))))))

(deftest test-line-column-coordinates
  (testing "line and column from point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \newline)
                  (b/insert-char \b))]
      (is (= [0 0] (b/point-to-line-column buf 0)))
      (is (= [0 1] (b/point-to-line-column buf 1)))
      (is (= [1 0] (b/point-to-line-column buf 2)))
      (is (= [1 1] (b/point-to-line-column buf 3))))))

(deftest test-insert-newline
  (testing "inserting newline splits text into lines"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \newline)
                  (b/insert-char \b))]
      (is (= "a\nb" (:text buf)))
      (is (= 3 (:point buf))))))
