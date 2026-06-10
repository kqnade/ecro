(ns ecro.config-test
  (:require
    [clojure.test :refer :all]
    [ecro.config :as cfg]))


(deftest test-load-config
  (testing "loading valid EDN config"
    (let [test-file (str (System/getProperty "java.io.tmpdir") "/ecro_config_" (System/currentTimeMillis) ".edn")
          config {:theme :dark
                  :keymap {"C-x C-s" :save-buffer}}]
      (try
        (spit test-file (pr-str config))
        (let [loaded (cfg/load-config test-file)]
          (is (= :dark (:theme loaded)))
          (is (map? (:keymap loaded)))
          (is (= :save-buffer (get-in loaded [:keymap "C-x C-s"]))))
        (finally
          (clojure.java.io/delete-file test-file true))))))


(deftest test-load-nonexistent-config
  (testing "loading nonexistent config returns empty map"
    (let [loaded (cfg/load-config "/tmp/nonexistent_ecro_config.edn")]
      (is (= {} loaded)))))


(deftest test-merge-config
  (testing "merging config into editor state"
    (let [state {:keymap {} :theme :light}
          config {:theme :dark :keymap {"C-x C-f" :find-file}}
          merged (cfg/merge-config state config)]
      (is (= :dark (:theme merged)))
      (is (= :find-file (get-in merged [:keymap "C-x C-f"]))))))


(deftest test-default-config
  (testing "default config has expected keys"
    (let [default (cfg/default-config)]
      (is (contains? default :theme))
      (is (contains? default :keymap)))))
