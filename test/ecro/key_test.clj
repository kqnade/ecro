(ns ecro.key-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [ecro.bindings :as bindings]
    [ecro.buffer :as b]
    [ecro.key :as key]
    [ecro.native :as native]
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
          state' (key/handle-key state 1004 key/shift-modifier)
          state'' (key/handle-key state' 1004 key/shift-modifier)
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
