(ns ecro.render-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.native :as native]
    [ecro.render :as render]
    [ecro.skk.state :as skk-state]))


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
      (is (str/starts-with? rendered "\033[7mhello"))
      (is (str/includes? rendered "\033[7mhello\033[0m"))
      (is (str/includes? rendered " world"))))
  (testing "line outside region is rendered normally"
    (let [line "hello world"
          rendered (#'render/render-line-with-region line 20 [0 5] 80 2)]
      (is (not (str/includes? rendered "\033[7m"))))))


(deftest test-render-line-with-region-fits-visible-width
  (testing "ANSI region highlighting does not consume terminal cells"
    (is (= "\033[7mhello\033[0m world"
           (#'render/render-line-with-region "hello world" 0 [0 5] 11 2)))))


(deftest test-render-screen-buffer-keeps-region-state
  (testing "screen-buffer stores highlighted lines so clearing mark redraws them"
    (let [marked-buf (assoc (b/make-buffer "test")
                            :text "hello world"
                            :point 5
                            :mark 0
                            :saved-text "hello world")
          plain-buf (assoc marked-buf :mark nil)
          marked-state {:current-buffer marked-buf :key-sequence []}
          plain-state {:current-buffer plain-buf :key-sequence []}]
      (with-redefs [native/get-terminal-size (fn [] [40 5])]
        (render/reset-screen-buffer!)
        (with-out-str (render/render marked-state))
        (is (str/includes? (first @render/screen-buffer) "\033[7mhello\033[0m"))
        (let [out (with-out-str (render/render plain-state))]
          (is (str/includes? out "hello world")))))))


(deftest test-render-cursor-uses-terminal-cell-width
  (testing "cursor position accounts for wide characters before point"
    (let [buf (assoc (b/make-buffer "test")
                     :text "日本"
                     :point 1
                     :saved-text "日本")
          state {:current-buffer buf :key-sequence []}]
      (with-redefs [native/get-terminal-size (fn [] [10 3])]
        (render/reset-screen-buffer!)
        (let [out (with-out-str (render/render state))]
          (is (str/includes? out "\033[1;3H\033[?25h")))))))


(deftest test-status-line-truncated-to-width
  (testing "status line is truncated to given width"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :name "very-long-file-name-that-exceeds-width")
                 :key-sequence []
                 :message nil}
          line (render/status-line state)]
      (is (= 10 (count (render/screen-line line 10 2))))
      (is (= 20 (count (render/screen-line line 20 2)))))))


(deftest test-screen-line-uses-terminal-cell-width
  (testing "wide characters advance tab stops by two terminal cells"
    (is (= "日  本" (render/screen-line "日\t本" 6 4)))))


(deftest test-status-line-shows-skk-mode
  (testing "status line shows SKK hiragana mode"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :minor-modes #{:skk-mode})
                  (skk-state/ensure-state))
          state {:current-buffer buf :key-sequence [] :message nil}]
      (is (str/includes? (render/status-line state) "SKK:かな")))))
