(ns ecro.file-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [ecro.file :as f]))


(deftest test-read-file
  (testing "reading a file into a buffer"
    (let [test-file (str (System/getProperty "java.io.tmpdir") "/ecro_test_" (System/currentTimeMillis) ".txt")]
      (try
        (spit test-file "hello world")
        (let [buf (f/read-file test-file)]
          (is (= "hello world" (:text buf)))
          (is (= test-file (:filepath buf)))
          (is (= :text-mode (:mode buf))))
        (finally
          (io/delete-file test-file true))))))


(deftest test-write-file
  (testing "writing a buffer to a file"
    (let [test-file (str (System/getProperty "java.io.tmpdir") "/ecro_test_" (System/currentTimeMillis) ".txt")]
      (try
        (let [buf {:name "test"
                   :text "hello ecro"
                   :filepath test-file}]
          (f/write-file buf)
          (is (= "hello ecro" (slurp test-file))))
        (finally
          (io/delete-file test-file true))))))


(deftest test-find-file-command
  (testing "find-file creates a buffer from file"
    (let [test-file (str (System/getProperty "java.io.tmpdir") "/ecro_test_" (System/currentTimeMillis) ".txt")]
      (try
        (spit test-file "test content")
        (let [result (f/find-file test-file)]
          (is (= "test content" (:text result)))
          (is (= test-file (:filepath result))))
        (finally
          (io/delete-file test-file true))))))


(deftest test-write-file-as
  (testing "write-file-as writes to a new path and updates buffer filepath"
    (let [src-file (str (System/getProperty "java.io.tmpdir") "/ecro_test_src_" (System/currentTimeMillis) ".txt")
          dst-file (str (System/getProperty "java.io.tmpdir") "/ecro_test_dst_" (System/currentTimeMillis) ".txt")]
      (try
        (spit src-file "original content")
        (let [buf (f/read-file src-file)
              new-buf (f/write-file-as buf dst-file)]
          (is (= "original content" (slurp dst-file)))
          (is (= dst-file (:filepath new-buf)))
          (is (= (.getName (io/file dst-file)) (:name new-buf)))
          (is (= "original content" (:saved-text new-buf))))
        (finally
          (io/delete-file src-file true)
          (io/delete-file dst-file true))))))


(deftest test-save-buffer-command
  (testing "save-buffer writes buffer to its filepath"
    (let [test-file (str (System/getProperty "java.io.tmpdir") "/ecro_test_" (System/currentTimeMillis) ".txt")]
      (try
        (let [buf {:name "test"
                   :text "saved content"
                   :filepath test-file}]
          (f/save-buffer buf)
          (is (= "saved content" (slurp test-file))))
        (finally
          (io/delete-file test-file true))))))


(deftest test-read-nonexistent-file
  (testing "reading nonexistent file creates empty buffer"
    (let [buf (f/read-file "/tmp/nonexistent_ecro_file.txt")]
      (is (= "" (:text buf))))))


(deftest test-write-buffer-without-path
  (testing "writing buffer without filepath returns nil"
    (let [buf {:name "test"
               :text "no path"}]
      (is (nil? (f/write-file buf))))))
