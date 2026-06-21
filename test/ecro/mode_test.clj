(ns ecro.mode-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [ecro.mode :as mode]))


(deftest test-mode-from-extension
  (testing "known file extensions map to major modes"
    (is (= :clojure-mode (mode/mode-from-extension ".clj")))
    (is (= :clojure-mode (mode/mode-from-extension ".cljc")))
    (is (= :clojure-mode (mode/mode-from-extension ".cljs")))
    (is (= :markdown-mode (mode/mode-from-extension ".md")))
    (is (= :text-mode (mode/mode-from-extension ".txt"))))
  (testing "unknown extension falls back to fundamental-mode"
    (is (= :fundamental-mode (mode/mode-from-extension ".xyz")))
    (is (= :fundamental-mode (mode/mode-from-extension "")))))


(deftest test-mode-from-buffer-name
  (testing "buffer name without path uses extension detection"
    (is (= :clojure-mode (mode/mode-from-buffer-name "foo.clj")))
    (is (= :text-mode (mode/mode-from-buffer-name "README.txt")))
    (is (= :fundamental-mode (mode/mode-from-buffer-name "*scratch*"))))
  (testing "buffer name with path extracts extension from filename"
    (is (= :clojure-mode (mode/mode-from-buffer-name "/home/user/project/src/foo.clj")))
    (is (= :markdown-mode (mode/mode-from-buffer-name "docs/README.md")))))


(deftest test-buffer-mode
  (testing "buffer mode is derived from filepath when present"
    (is (= :clojure-mode (:mode (mode/set-buffer-mode {:filepath "src/foo.clj"}))))
    (is (= :markdown-mode (:mode (mode/set-buffer-mode {:filepath "/tmp/note.md"})))))
  (testing "buffer mode falls back to name when filepath is absent"
    (is (= :text-mode (:mode (mode/set-buffer-mode {:name "log.txt"}))))
    (is (= :fundamental-mode (:mode (mode/set-buffer-mode {:name "*scratch*}"})))))
  (testing "buffer mode preserves existing mode when already set"
    (is (= :special-mode (:mode (mode/set-buffer-mode {:filepath "foo.clj" :mode :special-mode}))))))
