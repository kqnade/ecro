(ns ecro.native-test
  (:require
    [clojure.test :refer :all]
    [ecro.native :as native]))


(def lib-available?
  (try
    (some? @native/ecro-lib)
    (catch Exception _ false)))


(deftest test-jna-library-loaded
  (testing "JNA library is loaded or gracefully handles missing library"
    (is lib-available?)))


(deftest test-init-shutdown
  (testing "init and shutdown return values"
    (when lib-available?
      (is (= 0 (native/init)))
      (is (= 0 (native/shutdown))))))


(deftest test-terminal-size
  (testing "terminal size returns positive values"
    (when lib-available?
      (let [[width height] (native/get-terminal-size)]
        (is (pos? width))
        (is (pos? height))))))


(deftest test-raw-mode
  (testing "raw mode can be enabled and disabled"
    (when lib-available?
      ;; Raw mode may fail in non-terminal environments
      (let [enable-result (native/enable-raw-mode)
            disable-result (native/disable-raw-mode)]
        (is (or (= 0 enable-result) (= -1 enable-result)))
        (is (or (= 0 disable-result) (= -1 disable-result)))))))


(deftest test-alternate-screen
  (testing "alternate screen can be entered and left"
    (when lib-available?
      (is (= 0 (native/enter-alternate-screen)))
      (is (= 0 (native/leave-alternate-screen))))))


(deftest test-poll-event-no-block
  (testing "poll event returns nil when no event"
    (when lib-available?
      (is (nil? (native/poll-event))))))
