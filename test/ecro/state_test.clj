(ns ecro.state-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.state :as state]
    [ecro.window :as window]))


(deftest test-initial-state-creates-window-state
  (testing "initial state displays the scratch buffer in its selected window"
    (let [editor-state (state/initial-state {})
          selected-window (window/selected-window (:frame editor-state))]
      (is (= (:id (:current-buffer editor-state)) (:buffer-id selected-window))))))


(deftest test-add-buffer-to-list
  (testing "adding a buffer appends to buffer list"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf1
                 :buffers [buf1]}
          new-state (state/add-buffer state buf2)]
      (is (= 2 (count (:buffers new-state))))
      (is (= "test.txt" (:name (last (:buffers new-state))))))))


(deftest test-switch-to-existing-buffer
  (testing "switch-to-buffer changes current buffer to existing one"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf1
                 :buffers [buf1 buf2]}
          new-state (state/switch-to-buffer state "test.txt")]
      (is (= "test.txt" (:name (:current-buffer new-state)))))))


(deftest test-switch-to-buffer-creates-new
  (testing "switch-to-buffer creates new buffer if name not found"
    (let [buf1 (b/make-buffer "*scratch*")
          state {:current-buffer buf1
                 :buffers [buf1]}
          new-state (state/switch-to-buffer state "new.txt")]
      (is (= "new.txt" (:name (:current-buffer new-state))))
      (is (= 2 (count (:buffers new-state)))))))


(deftest test-kill-buffer
  (testing "kill-buffer removes buffer from list and switches to another"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf2
                 :buffers [buf1 buf2]}
          new-state (state/kill-buffer state "test.txt")]
      (is (= 1 (count (:buffers new-state))))
      (is (= "*scratch*" (:name (:current-buffer new-state)))))))


(deftest test-kill-buffer-keeps-last
  (testing "kill-buffer keeps last buffer and shows message"
    (let [buf1 (b/make-buffer "*scratch*")
          state {:current-buffer buf1
                 :buffers [buf1]}
          new-state (state/kill-buffer state "*scratch*")]
      (is (= 1 (count (:buffers new-state))))
      (is (= "Can't kill last buffer" (:message new-state))))))


(deftest test-kill-buffer-reassigns-windows-showing-it
  (testing "killing a buffer reassigns every window that displays it"
    (let [editor-state (state/initial-state {})
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
          killed-state (state/kill-buffer second-shows-other (:name scratch-buffer))
          windows-after-kill (window/get-windows (:frame killed-state))
          first-window-after-kill (first windows-after-kill)
          reselected-state (state/select-window killed-state first-window-after-kill)]
      (is (every? #(= (:id other-buffer) (:buffer-id %)) windows-after-kill))
      (is (= (:id other-buffer) (:id (:current-buffer reselected-state))))
      (is (not-any? #(= (:id scratch-buffer) (:id %)) (:buffers reselected-state))))))


(deftest test-list-buffers
  (testing "list-buffers creates a *Buffer List* buffer with names"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:current-buffer buf1 :buffers [buf1 buf2]}
          new-state (state/list-buffers state)]
      (is (= "*Buffer List*" (:name (:current-buffer new-state))))
      (is (= "*scratch*\ntest.txt" (:text (:current-buffer new-state))))
      (is (= "2 buffers" (:message new-state)))))
  (testing "list-buffers reuses existing *Buffer List* buffer"
    (let [buf1 (b/make-buffer "*scratch*")
          list-buf (b/make-buffer "*Buffer List*")
          state {:current-buffer list-buf :buffers [buf1 list-buf]}
          new-state (state/list-buffers state)]
      (is (= 2 (count (:buffers new-state))))
      (is (= "*scratch*" (:text (:current-buffer new-state)))))))


(deftest test-get-buffer-names
  (testing "get-buffer-names returns list of buffer names"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "test.txt")
          state {:buffers [buf1 buf2]}]
      (is (= ["*scratch*" "test.txt"] (state/get-buffer-names state))))))


(deftest test-current-buffer-edits-update-buffer-list
  (testing "editing current buffer keeps buffer list synchronized"
    (let [buf1 (b/make-buffer "*scratch*")
          buf2 (b/make-buffer "other.txt")
          state {:current-buffer buf1
                 :buffers [buf1 buf2]}
          edited (state/assoc-current-buffer state (b/insert-char buf1 \a))
          switched-away (state/switch-to-buffer edited "other.txt")
          switched-back (state/switch-to-buffer switched-away "*scratch*")]
      (is (= "a" (:text (:current-buffer edited))))
      (is (= "a" (:text (first (:buffers edited)))))
      (is (= "a" (:text (:current-buffer switched-back)))))))


(deftest test-current-buffer-edits-update-selected-window
  (testing "editing current buffer keeps the selected window synchronized"
    (let [editor-state (state/initial-state {})
          current-buffer (:current-buffer editor-state)
          edited-state (state/assoc-current-buffer editor-state
                                                   (b/insert-char current-buffer \a))]
      (is (= (:id (:current-buffer edited-state))
             (:buffer-id (window/selected-window (:frame edited-state))))))))


(deftest test-select-window-updates-current-buffer
  (testing "selecting a window updates current buffer and buffer list"
    (let [editor-state (state/initial-state {})
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
          current-second-window (second (window/get-windows (:frame first-selected)))
          selected-state (state/select-window first-selected current-second-window)]
      (is (= (:id other-buffer) (:id (:current-buffer selected-state))))
      (is (= (:id (:current-buffer selected-state))
             (:buffer-id (window/selected-window (:frame selected-state)))))
      (is (some #(= (:id other-buffer) (:id %)) (:buffers selected-state))))))


(deftest test-selecting-another-window-showing-same-buffer-preserves-edits
  (testing "selecting another window showing the current buffer does not restore a stale snapshot"
    (let [editor-state (state/initial-state {})
          frame (:frame editor-state)
          split-frame (window/split-window-vertical frame (:root-window frame))
          [first-window second-window] (window/get-windows split-frame)
          state-with-split (assoc editor-state :frame split-frame)
          second-selected (state/select-window state-with-split second-window)
          both-show-scratch (state/switch-to-buffer second-selected "*scratch*")
          first-selected (state/select-window both-show-scratch first-window)
          edited-state (state/assoc-current-buffer
                         first-selected
                         (b/insert-char (:current-buffer first-selected) \a))
          current-second-window (second (window/get-windows (:frame edited-state)))
          reselected-state (state/select-window edited-state current-second-window)
          scratch-buffer (first (filter #(= "*scratch*" (:name %))
                                        (:buffers reselected-state)))]
      (is (= "a" (:text (:current-buffer reselected-state))))
      (is (= "a" (:text scratch-buffer))))))


(deftest test-delete-selected-window-synchronizes-current-buffer
  (testing "deleting the selected window selects the remaining window buffer"
    (let [editor-state (state/initial-state {})
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
          selected-window (window/selected-window (:frame second-shows-other))
          deleted-state (state/delete-window second-shows-other selected-window)]
      (is (= 1 (count (window/get-windows (:frame deleted-state)))))
      (is (= (:id scratch-buffer) (:id (:current-buffer deleted-state))))
      (is (= (:id scratch-buffer)
             (:buffer-id (window/selected-window (:frame deleted-state))))))))
