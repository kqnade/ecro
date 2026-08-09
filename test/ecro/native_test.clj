(ns ecro.native-test
  (:require
    [clojure.test :refer :all]
    [ecro.native :as native]))


(def lib-available?
  (try
    (some? @native/ecro-lib)
    (catch Exception _ false)))


(def resource-patterns
  (->> (slurp "resources/META-INF/native-image/resource-config.json")
       (re-seq #"\"pattern\"\s*:\s*\"([^\"]+)\"")
       (map (comp re-pattern second))))


(def jni-config
  (slurp "resources/META-INF/native-image/jni-config.json"))


(deftest jna-dispatch-libraries-are-included-in-native-image
  (testing "JNA dispatch libraries for release platforms match an included resource pattern"
    (doseq [resource ["com/sun/jna/linux-x86-64/libjnidispatch.so"
                      "com/sun/jna/darwin-x86-64/libjnidispatch.jnilib"
                      "com/sun/jna/darwin-aarch64/libjnidispatch.jnilib"]]
      (is (some #(re-matches % resource) resource-patterns)
          (str resource " is not included by resource-config.json")))))


(deftest jna-core-types-are-registered-for-jni
  (testing "JNA can resolve the Java and JNA types used by its native dispatcher"
    (doseq [class-name ["com.sun.jna.Native"
                        "com.sun.jna.Pointer"
                        "com.sun.jna.Structure"
                        "java.lang.Object"]]
      (is (re-find (re-pattern (str "\\\"name\\\"\\s*:\\s*\\\""
                                    (java.util.regex.Pattern/quote class-name)
                                    "\\\""))
                   jni-config)
          (str class-name " is not registered in jni-config.json")))))


(deftest ecro-native-interface-proxy-is-registered
  (testing "JNA can create the native interface proxy in the native image"
    (let [metadata-file (java.io.File.
                          "resources/META-INF/native-image/reachability-metadata.json")]
      (is (.exists metadata-file)
          "reachability-metadata.json does not exist")
      (when (.exists metadata-file)
        (let [metadata (slurp metadata-file)]
          (is (re-find #"ecro\.native\.IEcroNative" metadata)
              "IEcroNative proxy is not registered")
          (doseq [method-name ["ecro_disable_raw_mode"
                               "ecro_enable_raw_mode"
                               "ecro_enter_alternate_screen"
                               "ecro_free_event"
                               "ecro_get_terminal_size"
                               "ecro_init"
                               "ecro_leave_alternate_screen"
                               "ecro_poll_event"
                               "ecro_read_event"
                               "ecro_shutdown"]]
            (is (re-find (re-pattern (str "\\\"name\\\"\\s*:\\s*\\\""
                                          method-name
                                          "\\\""))
                         metadata)
                (str method-name " is not registered for reflection"))))))))


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
