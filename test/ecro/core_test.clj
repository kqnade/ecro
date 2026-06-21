(ns ecro.core-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.core :as core]))


(deftest test-beginning-of-buffer
  (testing "M-< moves point to start of buffer"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello world")
                  (b/move-point-backward)
                  (core/beginning-of-buffer))]
      (is (= 0 (:point buf))))))


(deftest test-end-of-buffer
  (testing "M-> moves point to end of buffer"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello world")
                  (b/move-point-backward)
                  (core/end-of-buffer))]
      (is (= 11 (:point buf))))))


(deftest test-set-mark-command
  (testing "C-SPC sets the mark at current point"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello world")
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (core/set-mark-command))]
      (is (= 6 (:mark buf)))))
  (testing "second C-SPC deactivates the mark"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello")
                  (core/set-mark-command)
                  (core/set-mark-command))]
      (is (nil? (:mark buf))))))


(deftest test-jna-available
  (testing "JNA is available on classpath"
    (is (try
          (Class/forName "com.sun.jna.Library")
          true
          (catch ClassNotFoundException _
            false)))))


(deftest test-core-namespace-loads
  (testing "ecro.core namespace loads without errors"
    (is (find-ns 'ecro.core))))


(deftest test-forward-char
  (testing "C-f moves point forward"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/move-point-backward)
                  (core/forward-char))]
      (is (= 1 (:point buf))))))


(deftest test-forward-char-at-end
  (testing "C-f at end of buffer stays at end"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (core/forward-char))]
      (is (= 1 (:point buf))))))


(deftest test-backward-char
  (testing "C-b moves point backward"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (core/backward-char))]
      (is (= 1 (:point buf))))))


(deftest test-backward-char-at-beginning
  (testing "C-b at beginning stays at beginning"
    (let [buf (-> (b/make-buffer "test")
                  (core/backward-char))]
      (is (= 0 (:point buf))))))


(deftest test-next-line
  (testing "C-n moves to next line"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \newline)
                  (b/insert-char \b)
                  (b/insert-char \newline)
                  (b/insert-char \c)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (core/next-line))]
      (is (= 4 (:point buf))))))


(deftest test-previous-line
  (testing "C-p moves to previous line"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \newline)
                  (b/insert-char \b)
                  (core/next-line)
                  (core/previous-line))]
      (is (= 1 (:point buf))))))


(deftest test-move-beginning-of-line
  (testing "C-a moves to beginning of line"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (core/move-beginning-of-line))]
      (is (= 0 (:point buf))))))


(deftest test-move-end-of-line
  (testing "C-e moves to end of line"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (b/move-point-backward)
                  (core/move-end-of-line))]
      (is (= 2 (:point buf))))))


(deftest test-kill-line
  (testing "C-k kills from point to end of line"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-char \a)
                  (b/insert-char \b)
                  (b/insert-char \c)
                  (b/move-point-backward)
                  (core/kill-line))]
      (is (= "ab" (:text buf))))))


(deftest test-forward-word
  (testing "M-f moves to end of next word"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello world")
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (core/forward-word))]
      (is (= 11 (:point buf)))))
  (testing "M-f skips non-word characters before moving to word"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "  hello")
                  (core/forward-word))]
      (is (= 7 (:point buf)))))
  (testing "M-f stops at end of buffer"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hi")
                  (core/forward-word))]
      (is (= 2 (:point buf))))))


(deftest test-backward-word
  (testing "M-b moves to beginning of previous word"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello world")
                  (core/backward-word))]
      (is (= 6 (:point buf)))))
  (testing "M-b skips non-word characters"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hello  ")
                  (core/backward-word))]
      (is (= 0 (:point buf)))))
  (testing "M-b stops at beginning of buffer"
    (let [buf (-> (b/make-buffer "test")
                  (b/insert-text "hi")
                  (b/move-point-backward)
                  (b/move-point-backward)
                  (core/backward-word))]
      (is (= 0 (:point buf))))))
