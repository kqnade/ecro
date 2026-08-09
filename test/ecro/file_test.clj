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
      AclFileAttributeView
      FileAttribute
      GroupPrincipal
      PosixFileAttributeView
      PosixFileAttributes
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


(deftest test-write-file-through-symlink-replaces-referent
  (testing "writing through a symlink preserves the link and replaces its referent"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_symlink_"
                                              (make-array FileAttribute 0))
          referent (.resolve test-dir "referent.txt")
          symlink (.resolve test-dir "link.txt")]
      (try
        (spit (.toFile referent) "old content")
        (Files/createSymbolicLink symlink
                                  (.getFileName referent)
                                  (make-array FileAttribute 0))
        (f/write-file {:name "link.txt"
                       :text "new content"
                       :filepath (str symlink)})
        (is (Files/isSymbolicLink symlink))
        (is (= "new content" (slurp (.toFile referent))))
        (finally
          (Files/deleteIfExists symlink)
          (Files/deleteIfExists referent)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-through-broken-symlink-creates-referent
  (testing "writing through a broken symlink preserves the link and creates its referent"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_broken_symlink_"
                                              (make-array FileAttribute 0))
          referent (.resolve test-dir "referent.txt")
          symlink (.resolve test-dir "link.txt")]
      (try
        (Files/createSymbolicLink symlink
                                  (.getFileName referent)
                                  (make-array FileAttribute 0))
        (f/write-file {:name "link.txt"
                       :text "new content"
                       :filepath (str symlink)})
        (is (Files/isSymbolicLink symlink))
        (is (= "new content" (slurp (.toFile referent))))
        (finally
          (Files/deleteIfExists symlink)
          (Files/deleteIfExists referent)
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


(deftest test-write-file-creates-temp-with-existing-permissions
  (testing "the temporary file is never created broader than the existing target"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_initial_permissions_"
                                              (make-array FileAttribute 0))
          test-file (.resolve test-dir "target.txt")
          permissions (PosixFilePermissions/fromString "rw-------")
          initial-temp-permissions (atom nil)
          preserve-var (ns-resolve 'ecro.file 'preserve-posix-attributes)]
      (try
        (spit (.toFile test-file) "old content")
        (Files/setPosixFilePermissions test-file permissions)
        (with-redefs-fn {preserve-var
                         (fn [_ temp-file]
                           (reset! initial-temp-permissions
                                   (Files/getPosixFilePermissions temp-file
                                                                  (make-array LinkOption 0))))}
          #(f/write-file {:name "target.txt"
                          :text "new content"
                          :filepath (str test-file)}))
        (is (= permissions @initial-temp-permissions))
        (finally
          (Files/deleteIfExists test-file)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-preserves-existing-posix-ownership
  (testing "atomic replacement copies the existing POSIX owner and group"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_ownership_"
                                              (make-array FileAttribute 0))
          test-file (.resolve test-dir "target.txt")
          owner (reify java.nio.file.attribute.UserPrincipal
                  (getName [_] "original-owner"))
          group (reify GroupPrincipal
                  (getName [_] "original-group"))
          permissions (PosixFilePermissions/fromString "rw-------")
          attributes (reify PosixFileAttributes
                       (owner [_] owner)

                       (group [_] group)

                       (permissions [_] permissions))
          copied-attributes (atom {})
          source-view (reify PosixFileAttributeView
                        (readAttributes [_] attributes))
          target-view (reify PosixFileAttributeView
                        (setPermissions
                          [_ value]
                          (swap! copied-attributes assoc :permissions value))

                        (setGroup
                          [_ value]
                          (swap! copied-attributes assoc :group value))

                        (setOwner
                          [_ value]
                          (swap! copied-attributes assoc :owner value)))
          attribute-view-var (ns-resolve 'ecro.file 'posix-attribute-view)]
      (try
        (spit (.toFile test-file) "old content")
        (with-redefs-fn {attribute-view-var
                         (fn [path]
                           (if (= test-file path) source-view target-view))}
          #(f/write-file {:name "target.txt"
                          :text "new content"
                          :filepath (str test-file)}))
        (is (= {:permissions permissions
                :group group
                :owner owner}
               @copied-attributes))
        (finally
          (Files/deleteIfExists test-file)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-preserves-existing-acl
  (testing "atomic replacement copies the existing ACL and owner when supported"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_acl_"
                                              (make-array FileAttribute 0))
          test-file (.resolve test-dir "target.txt")
          owner (reify java.nio.file.attribute.UserPrincipal
                  (getName [_] "original-owner"))
          acl (java.util.ArrayList.)
          copied-attributes (atom {})
          source-view (reify AclFileAttributeView
                        (getOwner [_] owner)

                        (getAcl [_] acl))
          target-view (reify AclFileAttributeView
                        (setAcl
                          [_ value]
                          (swap! copied-attributes assoc :acl value))

                        (setOwner
                          [_ value]
                          (swap! copied-attributes assoc :owner value)))
          attribute-view-var (ns-resolve 'ecro.file 'file-attribute-view)]
      (try
        (spit (.toFile test-file) "old content")
        (with-redefs-fn {attribute-view-var
                         (fn [path attribute-view-class]
                           (when (= AclFileAttributeView attribute-view-class)
                             (if (= test-file path) source-view target-view)))}
          #(f/write-file {:name "target.txt"
                          :text "new content"
                          :filepath (str test-file)}))
        (is (= {:acl acl
                :owner owner}
               @copied-attributes))
        (finally
          (Files/deleteIfExists test-file)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-uses-normal-permissions-for-new-file
  (testing "a new atomic-save target uses the normal file creation permissions"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_new_permissions_"
                                              (make-array FileAttribute 0))
          reference-file (.resolve test-dir "reference.txt")
          test-file (.resolve test-dir "target.txt")]
      (try
        (spit (.toFile reference-file) "reference")
        (f/write-file {:name "target.txt"
                       :text "new content"
                       :filepath (str test-file)})
        (is (= (Files/getPosixFilePermissions reference-file
                                              (make-array LinkOption 0))
               (Files/getPosixFilePermissions test-file
                                              (make-array LinkOption 0))))
        (finally
          (Files/deleteIfExists test-file)
          (Files/deleteIfExists reference-file)
          (Files/deleteIfExists test-dir))))))


(deftest test-write-file-with-long-target-name
  (testing "atomic save keeps its temporary filename below the component limit"
    (let [test-dir (Files/createTempDirectory "ecro_atomic_save_long_name_"
                                              (make-array FileAttribute 0))
          filename (str (apply str (repeat 220 "a")) ".txt")
          test-file (.resolve test-dir filename)]
      (try
        (spit (.toFile test-file) "old content")
        (f/write-file {:name filename
                       :text "new content"
                       :filepath (str test-file)})
        (is (= "new content" (slurp (.toFile test-file))))
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
