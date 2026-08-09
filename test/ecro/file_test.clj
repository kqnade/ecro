(ns ecro.file-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [ecro.file :as f])
  (:import
    (java.io
      IOException)
    (java.nio.file
      Files
      LinkOption)
    (java.nio.file.attribute
      FileAttribute
      PosixFilePermissions)))


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


(deftest test-write-file-atomically-replaces-existing-file
  (testing "writing replaces the file instead of modifying it in place"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_"
                                              (make-array FileAttribute 0))
          test-file (.resolve test-dir "target.txt")
          old-file-link (.resolve test-dir "old-target.txt")]
      (try
        (spit (.toFile test-file) "old content")
        (Files/createLink old-file-link test-file)
        (f/write-file {:name "target.txt"
                       :text "new content"
                       :filepath (str test-file)})
        (is (= "new content" (slurp (.toFile test-file))))
        (is (= "old content" (slurp (.toFile old-file-link))))
        (finally
          (Files/deleteIfExists old-file-link)
          (Files/deleteIfExists test-file)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-preserves-existing-permissions
  (testing "atomic replacement keeps the existing file permissions"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_permissions_"
                                              (make-array FileAttribute 0))
          test-file (.resolve test-dir "target.txt")
          permissions (PosixFilePermissions/fromString "rw-r-----")]
      (try
        (spit (.toFile test-file) "old content")
        (Files/setPosixFilePermissions test-file permissions)
        (f/write-file {:name "target.txt"
                       :text "new content"
                       :filepath (str test-file)})
        (is (= permissions
               (Files/getPosixFilePermissions test-file
                                              (make-array LinkOption 0))))
        (finally
          (Files/deleteIfExists test-file)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-cleans-up-after-replacement-failure
  (testing "a failed atomic replacement preserves the target and removes its temporary file"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_failure_"
                                              (make-array FileAttribute 0))
          target-dir (.resolve test-dir "target.txt")
          target-file (.resolve target-dir "existing.txt")]
      (try
        (Files/createDirectory target-dir (make-array FileAttribute 0))
        (spit (.toFile target-file) "old content")
        (is (thrown? IOException
              (f/write-file {:name "target.txt"
                             :text "new content"
                             :filepath (str target-dir)})))
        (is (= "old content" (slurp (.toFile target-file))))
        (is (= #{"target.txt"}
               (set (seq (.list (.toFile test-dir))))))
        (finally
          (Files/deleteIfExists target-file)
          (Files/deleteIfExists target-dir)
          (Files/deleteIfExists test-dir))))))


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


(deftest test-write-file-as-atomically-replaces-existing-file
  (testing "write-file-as replaces the destination instead of modifying it in place"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_write_file_as_"
                                              (make-array FileAttribute 0))
          destination (.resolve test-dir "destination.txt")
          old-destination-link (.resolve test-dir "old-destination.txt")]
      (try
        (spit (.toFile destination) "old content")
        (Files/createLink old-destination-link destination)
        (f/write-file-as {:name "source.txt"
                          :text "new content"}
                         (str destination))
        (is (= "new content" (slurp (.toFile destination))))
        (is (= "old content" (slurp (.toFile old-destination-link))))
        (finally
          (Files/deleteIfExists old-destination-link)
          (Files/deleteIfExists destination)
          (Files/deleteIfExists test-dir))))))


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
