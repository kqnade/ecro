(ns ecro.buffer-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.undo :as undo]))


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


(deftest test-default-tab-inserts-two-spaces
  (testing "tab inserts two spaces by default"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-tab))]
      (is (= "  " (:text buf)))
      (is (= 2 (:point buf))))))


(deftest test-configured-tab-width
  (testing "tab width can be configured"
    (let [buf (-> (b/make-buffer "test" {:tab-width 4})
                  (b/insert-tab))]
      (is (= "    " (:text buf)))
      (is (= 4 (:point buf))))))


(deftest test-indent-tabs-mode-inserts-tab-character
  (testing "indent-tabs-mode inserts a literal tab"
    (let [buf (-> (b/make-buffer "test" {:indent-tabs-mode true})
                  (b/insert-tab))]
      (is (= "\t" (:text buf)))
      (is (= 1 (:point buf))))))


;; Undo/Redo tests
(deftest test-undo-insert
  (testing "undo single character insert"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (undo/undo))]
      (is (= "" (:text buf)))
      (is (= 0 (:point buf))))))


(deftest test-undo-multiple-inserts
  (testing "undo multiple inserts"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (b/insert-char \c)
                  (undo/undo))]
      (is (= "ab" (:text buf)))
      (is (= 2 (:point buf))))))


(deftest test-redo-insert
  (testing "redo after undo restores text"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (undo/undo)
                  (undo/redo))]
      (is (= "a" (:text buf)))
      (is (= 1 (:point buf))))))


(deftest test-undo-delete
  (testing "undo delete restores deleted character"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (b/move-point-backward)
                  (b/delete-char-forward)
                  (undo/undo))]
      (is (= "ab" (:text buf)))
      (is (= 2 (:point buf))))))


(deftest test-undo-stack-boundary
  (testing "undo at empty stack does nothing"
    (let [buf (b/make-buffer "test")]
      (is (= buf (undo/undo buf))))))


(deftest test-redo-stack-boundary
  (testing "redo with empty redo stack does nothing"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a))]
      (is (= buf (undo/redo buf))))))


(deftest test-new-operation-clears-redo-stack
  (testing "new operation after undo clears redo stack"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (undo/undo)
                  (b/insert-char \c))]
      (is (= "ac" (:text buf)))
      (is (= 2 (:point buf))))))


;; Mark/Region tests
(deftest test-set-mark
  (testing "set-mark stores current point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (b/set-mark))]
      (is (= 2 (:mark buf))))))


(deftest test-deactivate-mark
  (testing "deactivate-mark clears mark"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/set-mark)
                  (b/deactivate-mark))]
      (is (nil? (:mark buf))))))


(deftest test-mark-region-text
  (testing "region-text returns text between mark and point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \h)
                  (b/insert-char \e)
                  (b/insert-char \l)
                  (b/insert-char \l)
                  (b/insert-char \o)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/set-mark)
                  (b/move-point-forward)
                  (b/move-point-forward))]
      (is (= "lo" (b/region-text buf))))))
