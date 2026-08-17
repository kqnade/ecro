(ns ecro.key-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [ecro.bindings :as bindings]
    [ecro.buffer :as b]
    [ecro.key :as key]
    [ecro.native :as native]
    [ecro.render :as render]
    [ecro.state :as state]
    [ecro.window :as window]))


(deftest test-key-name-control-shift-and-control-slash
  (testing "terminal key codes map to keymap names"
    (is (= "C-z" (key/key-name (int \z) 1)))
    (is (= "C-S-z" (key/key-name (int \Z) 5)))
    (is (= "C-/" (key/key-name (int \/) 1)))
    (is (= "RET" (key/key-name 13 0)))
    (is (= "C-m" (key/key-name (int \m) 1)))
    (is (= "TAB" (key/key-name 9 0)))
    (is (= "C-i" (key/key-name (int \i) 1)))))


(deftest test-key-name-alt
  (testing "ALT modifier produces M- prefix"
    (is (= "M-f" (key/key-name 102 2)))
    (is (= "M-b" (key/key-name 98 2)))
    (is (= "M-x" (key/key-name 120 2)))))


(deftest test-handle-key-esc-prefix
  (testing "ESC starts prefix sequence"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          new-state (key/handle-key state 27 0)] ; ESC
      (is (= ["ESC"] (:key-sequence new-state))))))


(deftest test-handle-key-esc-prefix-complete
  (testing "ESC f activates find-file minibuffer"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence ["ESC"]}
          new-state (key/handle-key state 102 0)] ; f after ESC
      (is (= [] (:key-sequence new-state)))
      (is (some? (:minibuffer new-state)))
      (is (= "Find file: " (get-in new-state [:minibuffer :prompt]))))))


(deftest test-handle-key-deletes-selected-window
  (testing "ESC 0 deletes the selected window and synchronizes current buffer"
    (let [editor-state (state/initial-state bindings/default-keymap)
          scratch-buffer (:current-buffer editor-state)
          other-buffer (b/make-buffer "other.txt")
          frame (:frame editor-state)
          split-frame (window/split-window-vertical frame (:root-window frame))
          second-window (second (window/get-windows split-frame))
          state-with-split (-> editor-state
                               (state/add-buffer other-buffer)
                               (assoc :frame split-frame))
          second-selected (state/select-window state-with-split second-window)
          second-shows-other (state/assoc-current-buffer second-selected other-buffer)
          deleted-state (key/handle-key (assoc second-shows-other :key-sequence ["ESC"])
                                        (int \0)
                                        0)]
      (is (= 1 (count (window/get-windows (:frame deleted-state)))))
      (is (= (:id scratch-buffer) (:id (:current-buffer deleted-state))))
      (is (= [] (:key-sequence deleted-state))))))


(deftest test-handle-key-deletes-other-windows
  (testing "ESC 1 keeps the selected window and its current buffer"
    (let [editor-state (state/initial-state bindings/default-keymap)
          other-buffer (b/make-buffer "other.txt")
          frame (:frame editor-state)
          split-frame (window/split-window-vertical frame (:root-window frame))
          second-window (second (window/get-windows split-frame))
          state-with-split (-> editor-state
                               (state/add-buffer other-buffer)
                               (assoc :frame split-frame))
          second-selected (state/select-window state-with-split second-window)
          second-shows-other (state/assoc-current-buffer second-selected other-buffer)
          single-window-state (key/handle-key (assoc second-shows-other :key-sequence ["ESC"])
                                              (int \1)
                                              0)]
      (is (= 1 (count (window/get-windows (:frame single-window-state)))))
      (is (= (:id other-buffer) (:id (:current-buffer single-window-state))))
      (is (= (:id other-buffer)
             (:buffer-id (window/selected-window (:frame single-window-state)))))
      (is (= [] (:key-sequence single-window-state))))))


(deftest test-handle-key-selects-other-window
  (testing "ESC o selects the next window and synchronizes current buffer"
    (let [editor-state (state/initial-state bindings/default-keymap)
          scratch-buffer (:current-buffer editor-state)
          other-buffer (b/make-buffer "other.txt")
          frame (:frame editor-state)
          split-frame (window/split-window-vertical frame (:root-window frame))
          [first-window second-window] (window/get-windows split-frame)
          state-with-split (-> editor-state
                               (state/add-buffer other-buffer)
                               (assoc :frame split-frame))
          second-selected (state/select-window state-with-split second-window)
          second-shows-other (state/assoc-current-buffer second-selected other-buffer)
          first-selected (state/select-window second-shows-other first-window)
          selected-state (key/handle-key (assoc first-selected :key-sequence ["ESC"])
                                         (int \o)
                                         0)]
      (is (= (:id scratch-buffer) (:id (:current-buffer first-selected))))
      (is (= (:id other-buffer) (:id (:current-buffer selected-state))))
      (is (= (:id other-buffer)
             (:buffer-id (window/selected-window (:frame selected-state)))))
      (is (= [] (:key-sequence selected-state))))))


(deftest test-forward-incremental-search-integration
  (testing "C-s searches as characters are typed and RET accepts the match"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "hello world")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          with-w (key/handle-key started (int \w) 0)
          with-wo (key/handle-key with-w (int \o) 0)
          accepted (key/handle-key with-wo 13 0)]
      (is (= {:pattern "" :direction :forward :start-point 0}
             (:isearch started)))
      (is (= 6 (get-in with-w [:current-buffer :point])))
      (is (= "wo" (get-in with-wo [:isearch :pattern])))
      (is (= "I-search: wo" (render/status-line with-wo)))
      (is (= 6 (get-in accepted [:current-buffer :point])))
      (is (nil? (:isearch accepted))))))


(deftest test-backward-incremental-search-integration
  (testing "C-r searches backward as characters are typed"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "foo bar foo"
                                        :point 11)
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \r) 1)
          searched (key/handle-key started (int \b) 0)]
      (is (= :backward (get-in started [:isearch :direction])))
      (is (= "b" (get-in searched [:isearch :pattern])))
      (is (= 4 (get-in searched [:current-buffer :point])))
      (is (= "I-search backward: b" (render/status-line searched))))))


(deftest test-incremental-search-backspace
  (testing "BS removes the last query character and recomputes the match"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "hello world")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          searched (key/handle-key started (int \w) 0)
          cleared (key/handle-key searched 127 0)]
      (is (= 6 (get-in searched [:current-buffer :point])))
      (is (= "" (get-in cleared [:isearch :pattern])))
      (is (= 0 (get-in cleared [:current-buffer :point]))))))


(deftest test-incremental-search-cancel
  (testing "ESC cancels search and restores the starting point"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "hello world"
                                        :point 2)
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          searched (key/handle-key started (int \w) 0)
          canceled (key/handle-key searched 27 0)]
      (is (= 6 (get-in searched [:current-buffer :point])))
      (is (= 2 (get-in canceled [:current-buffer :point])))
      (is (nil? (:isearch canceled))))))


(deftest test-incremental-search-non-bmp-character
  (testing "a non-BMP code point can be added and removed as one character"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "a😀b")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          searched (key/handle-key started 0x1F600 0)
          cleared (key/handle-key searched 127 0)]
      (is (= "😀" (get-in searched [:isearch :pattern])))
      (is (= 1 (get-in searched [:current-buffer :point])))
      (is (= "" (get-in cleared [:isearch :pattern])))
      (is (= 0 (get-in cleared [:current-buffer :point]))))))


(deftest test-incremental-search-ignores-terminal-sentinel-codes
  (testing "navigation and function key sentinels do not enter the query"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "hello world")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          searched (key/handle-key started (int \w) 0)
          after-specials (reduce #(key/handle-key %1 %2 0)
                                 searched
                                 [1001 1004 1005 1010 2001])]
      (is (= "w" (get-in after-specials [:isearch :pattern])))
      (is (= 6 (get-in after-specials [:current-buffer :point]))))))


(deftest test-incremental-search-classifies-modifiers
  (testing "Shift text is accepted while unrelated Ctrl and Alt chords are ignored"
    (let [state {:current-buffer (assoc (b/make-buffer "test") :text "W")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          shifted (key/handle-key started (int \W) key/shift-modifier)
          after-chords (-> shifted
                           (key/handle-key (int \g) 1)
                           (key/handle-key (int \x) 2))]
      (is (= "W" (get-in after-chords [:isearch :pattern])))
      (is (= 0 (get-in after-chords [:current-buffer :point]))))))


(deftest test-incremental-search-repeat-controls
  (testing "C-s and C-r repeat the query without entering command characters"
    (let [state {:current-buffer (assoc (b/make-buffer "test")
                                        :text "foo foo foo")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          started (key/handle-key state (int \s) 1)
          searched (key/handle-key started (int \f) 0)
          next-match (key/handle-key searched (int \s) 1)
          refined (key/handle-key next-match (int \o) 0)
          previous-match (key/handle-key refined (int \r) 1)]
      (is (= 4 (get-in next-match [:current-buffer :point])))
      (is (= 4 (get-in refined [:current-buffer :point])))
      (is (= 0 (get-in previous-match [:current-buffer :point])))
      (is (= "fo" (get-in previous-match [:isearch :pattern])))
      (is (= :backward (get-in previous-match [:isearch :direction]))))))


(deftest test-handle-key-inserts-non-ascii-character
  (testing "a printable Unicode key inserts its character"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          new-state (key/handle-key state (int \日) 0)]
      (is (= "日" (get-in new-state [:current-buffer :text])))
      (is (= 1 (get-in new-state [:current-buffer :point]))))))


(deftest test-handle-key-inserts-character-that-matched-old-up-key
  (testing "U+03E9 is inserted instead of moving the cursor"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          new-state (key/handle-key state 0x03e9 0)]
      (is (= "ϩ" (get-in new-state [:current-buffer :text])))
      (is (= 1 (get-in new-state [:current-buffer :point]))))))


(deftest test-minibuffer-handles-supplementary-character-before-function-key
  (testing "a printable supplementary key inserts while F1 is ignored"
    (let [state {:minibuffer {:buffer (b/make-buffer " *minibuffer*")}}
          emoji-state (key/handle-key state 0x1f600 0)
          f1-state (key/handle-key emoji-state (inc Character/MAX_CODE_POINT) 0)]
      (is (= "😀" (get-in f1-state [:minibuffer :buffer :text])))
      (is (= 2 (get-in f1-state [:minibuffer :buffer :point]))))))


(deftest test-minibuffer-switch-to-buffer
  (testing "minibuffer Enter switches to named buffer"
    (let [state {:minibuffer {:buffer {:text "other.clj"}
                              :command :switch-to-buffer
                              :prompt "Switch to buffer: "}
                 :current-buffer {:name "*scratch*" :text "" :point 0}
                 :buffers [{:name "*scratch*" :text "" :point 0}]}
          new-state (key/handle-key state 13 0)]
      (is (nil? (:minibuffer new-state)))
      (is (= "other.clj" (:name (:current-buffer new-state)))))))


(deftest test-minibuffer-kill-buffer
  (testing "minibuffer Enter kills named buffer"
    (let [state {:minibuffer {:buffer {:text "other.clj"}
                              :command :kill-buffer
                              :prompt "Kill buffer: "}
                 :current-buffer {:name "*scratch*" :text "" :point 0}
                 :buffers [{:name "*scratch*" :text "" :point 0}
                           {:name "other.clj" :text "" :point 0}]}
          new-state (key/handle-key state 13 0)]
      (is (nil? (:minibuffer new-state)))
      (is (= 1 (count (:buffers new-state))))
      (is (= "*scratch*" (:name (:current-buffer new-state)))))))


(deftest test-minibuffer-write-file
  (testing "minibuffer Enter writes current buffer to given path"
    (let [tmp (str (System/getProperty "java.io.tmpdir") "/ecro_write_" (System/currentTimeMillis) ".txt")
          state {:minibuffer {:buffer {:text tmp}
                              :command :write-file
                              :prompt "Write file: "}
                 :current-buffer {:name "*scratch*" :text "hello" :point 0 :saved-text ""}
                 :buffers [{:name "*scratch*" :text "hello" :point 0 :saved-text ""}]}]
      (try
        (let [new-state (key/handle-key state 13 0)]
          (is (nil? (:minibuffer new-state)))
          (is (= "hello" (slurp tmp)))
          (is (= tmp (:filepath (:current-buffer new-state)))))
        (finally
          (io/delete-file tmp true))))))


(deftest test-repeated-shift-arrow-keeps-selection-buffer
  (testing "repeated Shift+Right extends selection without replacing buffer with mark"
    (let [state {:current-buffer (assoc (b/make-buffer "test") :text "abc")
                 :keymap bindings/default-keymap
                 :key-sequence []}
          state' (key/handle-key state key/right-key-code key/shift-modifier)
          state'' (key/handle-key state' key/right-key-code key/shift-modifier)
          buf (:current-buffer state'')]
      (is (= 0 (:mark buf)))
      (is (= 2 (:point buf)))
      (is (= "abc" (:text buf))))))


(deftest test-process-event-keeps-selected-window-buffer-synchronized
  (testing "scroll adjustment updates the selected window buffer"
    (let [editor-state (state/initial-state bindings/default-keymap)
          current-buffer (assoc (:current-buffer editor-state)
                                :text "1\n2\n3\n4\n5\n6\n7\n8\n9\n10"
                                :point 15)
          state-with-point (state/assoc-current-buffer editor-state current-buffer)
          processed-state (with-redefs [native/get-terminal-size (constantly [80 6])]
                            (key/process-event state-with-point nil))]
      (is (= 3 (:scroll-line (:current-buffer processed-state))))
      (is (= (:id (:current-buffer processed-state))
             (:buffer-id (window/selected-window (:frame processed-state))))))))
