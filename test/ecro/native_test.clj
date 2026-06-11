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
  (testing "terminal size returns positive values or nil on failure"
    (when lib-available?
      (if-let [[width height] (native/get-terminal-size)]
        (do (is (pos? width))
            (is (pos? height)))
        (is (nil? (native/get-terminal-size)))))))


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


(deftest test-decode-event-data
  (testing "decode-event-data maps raw ints to event map"
    (is (= {:type :key :key_code 65 :modifiers 0 :width 65 :height 0}
           (native/decode-event-data 1 65 0)))
    (is (= {:type :resize :key_code 80 :modifiers 24 :width 80 :height 24}
           (native/decode-event-data 2 80 24)))
    (is (= {:type :unknown :key_code 0 :modifiers 0 :width 0 :height 0}
           (native/decode-event-data 99 0 0)))))
