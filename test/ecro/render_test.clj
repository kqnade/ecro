(ns ecro.render-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.render :as render]))


(deftest test-status-line-shows-key-sequence
  (testing "status line displays key sequence without hardcoded C- prefix"
    (let [state {:current-buffer (b/make-buffer "test")
                 :key-sequence ["ESC"]
                 :message nil}]
      (is (str/includes? (render/status-line state) "  ESC "))))
  (testing "status line displays C-g lead key correctly"
    (let [state {:current-buffer (b/make-buffer "test")
                 :key-sequence ["C-g"]
                 :message nil}]
      (is (str/includes? (render/status-line state) "  C-g "))))
  (testing "status line omits key sequence when empty"
    (let [state {:current-buffer (b/make-buffer "test")
                 :key-sequence []
                 :message nil}
          line (render/status-line state)]
      (is (not (str/includes? line "ESC")))
      (is (not (str/includes? line "C-"))))))


(deftest test-render-line-with-region
  (testing "region is highlighted with reverse video escape sequences"
    (let [line "hello world"
          rendered (#'render/render-line-with-region line 0 [0 5] 80 2)]
      (is (str/includes? rendered "\033[7mhello\033[0m"))
      (is (str/includes? rendered " world"))))
  (testing "line outside region is rendered normally"
    (let [line "hello world"
          rendered (#'render/render-line-with-region line 20 [0 5] 80 2)]
      (is (not (str/includes? rendered "\033[7m"))))))


(deftest test-status-line-truncated-to-width
  (testing "status line is truncated to given width"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :name "very-long-file-name-that-exceeds-width")
                 :key-sequence []
                 :message nil}
          line (render/status-line state)]
      (is (= 10 (count (render/screen-line line 10 2))))
      (is (= 20 (count (render/screen-line line 20 2)))))))
