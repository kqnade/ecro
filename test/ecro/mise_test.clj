(ns ecro.mise-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [ecro.mise :as mise]))


(deftest test-find-mise-toml
  (testing "finds mise.toml in the same directory as the file"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")]
      (.mkdirs project)
      (spit (io/file project "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file project "src" "foo.clj") home)]
        (is (= (.getAbsolutePath (io/file project "mise.toml")) result))
        (.delete (io/file project "mise.toml"))
        (.delete project)
        (.delete home)
        (.delete tmp))))
  (testing "finds mise.toml in a parent directory"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")
          sub (io/file project "src")]
      (.mkdirs sub)
      (spit (io/file project "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file sub "foo.clj") home)]
        (is (= (.getAbsolutePath (io/file project "mise.toml")) result))
        (.delete (io/file project "mise.toml"))
        (.delete sub)
        (.delete project)
        (.delete home)
        (.delete tmp))))
  (testing "stops at git root"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          outer (io/file home "outer")
          inner (io/file outer "inner")
          project (io/file inner "project")]
      (.mkdirs project)
      (.mkdirs (io/file inner ".git"))
      (spit (io/file outer "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file project "foo.clj") home)]
        (is (nil? result))
        (.delete (io/file outer "mise.toml"))
        (.delete (io/file inner ".git"))
        (.delete project)
        (.delete inner)
        (.delete outer)
        (.delete home)
        (.delete tmp))))
  (testing "stops at HOME"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")
          parent (io/file tmp "above")]
      (.mkdirs project)
      (.mkdirs parent)
      (spit (io/file parent "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file project "foo.clj") home)]
        (is (nil? result))
        (.delete (io/file parent "mise.toml"))
        (.delete project)
        (.delete parent)
        (.delete home)
        (.delete tmp))))
  (testing "returns nil when no mise.toml exists"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")]
      (.mkdirs project)
      (let [result (mise/find-mise-toml (io/file project "foo.clj") home)]
        (is (nil? result))
        (.delete project)
        (.delete home)
        (.delete tmp)))))
