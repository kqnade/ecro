(ns ecro.skk.config-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [ecro.skk.config :as skk-config]))


(deftest test-load-jisyo-paths-default
  (testing "falls back to default ~/.skk-jisyo when no config or file exists"
    (let [tmp-home (System/getProperty "java.io.tmpdir")
          original-home (System/getProperty "user.home")]
      (try
        (System/setProperty "user.home" tmp-home)
        (let [default-file (io/file tmp-home ".skk-jisyo")]
          (.delete default-file)
          (is (= {:jisyo-path nil :large-jisyo-path nil}
                 (skk-config/load-jisyo-paths {})))
          (spit default-file ";; okuri-nasi entries.\n")
          (is (= (.getPath default-file) (:jisyo-path (skk-config/load-jisyo-paths {}))))
          (.delete default-file))
        (finally
          (System/setProperty "user.home" original-home))))))


(deftest test-load-jisyo-paths-from-ecro-config
  (testing "ecro config takes precedence over defaults"
    (let [tmp-home (System/getProperty "java.io.tmpdir")
          original-home (System/getProperty "user.home")
          custom-file (io/file tmp-home "custom-skk-jisyo")]
      (try
        (System/setProperty "user.home" tmp-home)
        (.delete custom-file)
        (spit custom-file ";; okuri-nasi entries.\n")
        (is (= (.getPath custom-file)
               (:jisyo-path (skk-config/load-jisyo-paths {:skk {:jisyo-path (.getPath custom-file)}}))))
        (.delete custom-file)
        (finally
          (System/setProperty "user.home" original-home))))))


(deftest test-load-jisyo-paths-from-skk-init
  (testing "reads skk-jisyo path from ~/.skk without evaluating Elisp"
    (let [tmp-home (System/getProperty "java.io.tmpdir")
          original-home (System/getProperty "user.home")
          skk-init (io/file tmp-home ".skk")
          jisyo-file (io/file tmp-home "from-skk-init")]
      (try
        (System/setProperty "user.home" tmp-home)
        (.delete skk-init)
        (.delete jisyo-file)
        (spit jisyo-file ";; okuri-nasi entries.\n")
        (spit skk-init "(setq skk-jisyo \"~/from-skk-init\")\n")
        (is (= (.getPath jisyo-file)
               (:jisyo-path (skk-config/load-jisyo-paths {}))))
        (.delete skk-init)
        (.delete jisyo-file)
        (finally
          (System/setProperty "user.home" original-home))))))


(deftest test-load-jisyo-paths-large-jisyo
  (testing "reads skk-large-jisyo path from ~/.skk"
    (let [tmp-home (System/getProperty "java.io.tmpdir")
          original-home (System/getProperty "user.home")
          skk-init (io/file tmp-home ".skk")
          large-file (io/file tmp-home "SKK-JISYO.L")]
      (try
        (System/setProperty "user.home" tmp-home)
        (.delete skk-init)
        (.delete large-file)
        (spit large-file ";; okuri-nasi entries.\n")
        (spit skk-init "(setq skk-large-jisyo \"~/SKK-JISYO.L\")\n")
        (is (= (.getPath large-file)
               (:large-jisyo-path (skk-config/load-jisyo-paths {}))))
        (.delete skk-init)
        (.delete large-file)
        (finally
          (System/setProperty "user.home" original-home))))))


(deftest test-ecro-config-takes-precedence-over-skk-init
  (testing "ecro config overrides ~/.skk values"
    (let [tmp-home (System/getProperty "java.io.tmpdir")
          original-home (System/getProperty "user.home")
          skk-init (io/file tmp-home ".skk")
          ecro-file (io/file tmp-home "ecro-jisyo")
          init-file (io/file tmp-home "init-jisyo")]
      (try
        (System/setProperty "user.home" tmp-home)
        (.delete skk-init)
        (.delete ecro-file)
        (.delete init-file)
        (spit ecro-file ";; okuri-nasi entries.\n")
        (spit init-file ";; okuri-nasi entries.\n")
        (spit skk-init "(setq skk-jisyo \"~/init-jisyo\")\n")
        (is (= (.getPath ecro-file)
               (:jisyo-path (skk-config/load-jisyo-paths {:skk {:jisyo-path (.getPath ecro-file)}}))))
        (.delete skk-init)
        (.delete ecro-file)
        (.delete init-file)
        (finally
          (System/setProperty "user.home" original-home))))))
