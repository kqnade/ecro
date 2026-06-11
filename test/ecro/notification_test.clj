(ns ecro.notification-test
  (:require
    [clojure.test :refer :all]
    [ecro.notification :as notification]))


(deftest test-notification-level-helpers
  (testing "notification helpers attach typed notifications"
    (is (= {:level :info :message "loaded"}
           (:notification (notification/info {} "loaded"))))
    (is (= {:level :warn :message "check this"}
           (:notification (notification/warn {} "check this"))))
    (is (= {:level :error :message "failed"}
           (:notification (notification/error {} "failed"))))
    (is (= {:level :debug :message "trace"}
           (:notification (notification/debug {} "trace"))))))


(deftest test-notification-text
  (testing "notification text includes level and message"
    (is (= "INFO: loaded"
           (notification/text {:level :info :message "loaded"})))
    (is (= "WARN: check this"
           (notification/text {:level :warn :message "check this"})))
    (is (= "ERROR: failed"
           (notification/text {:level :error :message "failed"})))
    (is (= "DEBUG: trace"
           (notification/text {:level :debug :message "trace"}))))
  (testing "nil notifications have no display text"
    (is (nil? (notification/text nil)))))


(deftest test-invalid-notification-level
  (testing "invalid notification levels fail fast"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid notification level"
          (notification/notify {} :fatal "boom")))))
