(ns ecro.window-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.window :as w]))


(deftest test-create-window
  (testing "window creation with buffer"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf)]
      (is (= (:id buf) (:buffer-id win)))
      (is (= 0 (:top win)))
      (is (= 0 (:left win)))
      (is (= 24 (:height win)))
      (is (= 80 (:width win))))))


(deftest test-create-frame
  (testing "frame creation with single window"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf)
          frame (w/make-frame win)]
      (is (= 24 (:height frame)))
      (is (= 80 (:width frame)))
      (is (= win (:root-window frame)))
      (is (= win (w/selected-window frame))))))


(deftest test-vertical-split
  (testing "splitting window vertically creates two windows side by side"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf 80 24)
          frame (w/make-frame win)
          new-frame (w/split-window-vertical frame (:root-window frame))]
      (is (= 2 (count (w/get-windows new-frame))))
      (is (= 80 (:width (:root-window new-frame))))
      (is (= 40 (:width (first (:children (:root-window new-frame)))))))))


(deftest test-vertical-split-accepts-stale-handle
  (testing "vertical split identifies its target by stable window ID"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          stale-window (assoc (:root-window frame) :top 99)
          split-frame (w/split-window-vertical frame stale-window)]
      (is (= 2 (count (w/get-windows split-frame)))))))


(deftest test-horizontal-split
  (testing "splitting window horizontally creates two windows stacked"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf 80 24)
          frame (w/make-frame win)
          new-frame (w/split-window-horizontal frame (:root-window frame))]
      (is (= 2 (count (w/get-windows new-frame))))
      (is (= 24 (:height (:root-window new-frame))))
      (is (= 12 (:height (first (:children (:root-window new-frame)))))))))


(deftest test-horizontal-split-accepts-stale-handle
  (testing "horizontal split identifies its target by stable window ID"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          stale-window (assoc (:root-window frame) :top 99)
          split-frame (w/split-window-horizontal frame stale-window)]
      (is (= 2 (count (w/get-windows split-frame)))))))


(deftest test-split-non-root-window-returns-unchanged
  (testing "splitting a non-root window returns the frame unchanged"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf 80 24)
          frame (w/make-frame win)
          frame2 (w/split-window-vertical frame (:root-window frame))
          wins (w/get-windows frame2)
          non-root (second wins)
          result (w/split-window-vertical frame2 non-root)]
      (is (= frame2 result))
      (is (= 2 (count (w/get-windows result)))))))


(deftest test-window-navigation
  (testing "navigating between windows"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf 80 24)
          frame (w/make-frame win)
          frame2 (w/split-window-vertical frame (:root-window frame))
          wins (w/get-windows frame2)]
      (is (= 2 (count wins)))
      (is (= (second wins) (w/next-window frame2 (first wins))))
      (is (= (first wins) (w/prev-window frame2 (second wins)))))))


(deftest test-next-window-accepts-stale-handle
  (testing "next-window identifies its starting point by stable window ID"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          stale-window (assoc (first (w/get-windows split-frame)) :top 99)
          expected-window (second (w/get-windows split-frame))]
      (is (= (:id expected-window)
             (:id (w/next-window split-frame stale-window)))))))


(deftest test-prev-window-accepts-stale-handle
  (testing "prev-window identifies its starting point by stable window ID"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          stale-window (assoc (second (w/get-windows split-frame)) :top 99)
          expected-window (first (w/get-windows split-frame))]
      (is (= (:id expected-window)
             (:id (w/prev-window split-frame stale-window)))))))


(deftest test-select-window
  (testing "selecting a window updates the frame selection"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          target-window (second (w/get-windows split-frame))
          selected-frame (w/select-window split-frame target-window)]
      (is (= target-window (w/selected-window selected-frame))))))


(deftest test-window-buffer-assignment
  (testing "assigning different buffers to windows"
    (let [buf1 (b/make-buffer "buffer1")
          buf2 (b/make-buffer "buffer2")
          win1 (w/make-window buf1)
          win2 (w/make-window buf2)]
      (is (= (:id buf1) (:buffer-id win1)))
      (is (= (:id buf2) (:buffer-id win2))))))


(deftest test-delete-window
  (testing "delete-window removes the given window"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          wins (w/get-windows split-frame)
          deleted (w/delete-window split-frame (first wins))]
      (is (= 1 (count (w/get-windows deleted)))))))


(deftest test-delete-window-accepts-stale-handle
  (testing "delete-window identifies its target by stable window ID"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          stale-target (assoc (first (w/get-windows split-frame)) :top 99)
          deleted-frame (w/delete-window split-frame stale-target)]
      (is (= 1 (count (w/get-windows deleted-frame)))))))


(deftest test-delete-only-window-does-not-restore-stale-handle
  (testing "delete-window keeps the current tree value when it cannot remove the last window"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          current-window (:root-window frame)
          stale-window (assoc current-window :top 99)
          unchanged-frame (w/delete-window frame stale-window)]
      (is (= current-window (:root-window unchanged-frame))))))


(deftest test-delete-other-windows
  (testing "delete-other-windows keeps only the given window"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          wins (w/get-windows split-frame)
          kept (w/delete-other-windows split-frame (second wins))]
      (is (= 1 (count (w/get-windows kept))))
      (is (= (:id buf) (-> kept :root-window :buffer-id)))
      (is (= 80 (-> kept :root-window :width)))
      (is (= 24 (-> kept :root-window :height)))
      (is (nil? (-> kept :root-window :parent))))))


(deftest test-other-window
  (testing "other-window cycles to the next window"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          wins (w/get-windows split-frame)]
      (is (= (second wins) (w/other-window split-frame (first wins))))
      (is (= (first wins) (w/other-window split-frame (second wins)))))))


(deftest test-frame-resize
  (testing "resizing frame updates root window"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf)
          frame (w/make-frame win)
          resized (w/resize-frame frame 100 40)]
      (is (= 100 (:width resized)))
      (is (= 40 (:height resized)))
      (is (= 100 (:width (:root-window resized))))
      (is (= 40 (:height (:root-window resized)))))))


(deftest test-frame-resize-preserves-selected-window
  (testing "resizing a split frame preserves its selected window"
    (let [buf (b/make-buffer "test")
          frame (w/make-frame (w/make-window buf 80 24))
          split-frame (w/split-window-vertical frame (:root-window frame))
          target-window (second (w/get-windows split-frame))
          selected-frame (w/select-window split-frame target-window)
          resized-frame (w/resize-frame selected-frame 100 40)]
      (is (= (:id target-window)
             (:id (w/selected-window resized-frame)))))))
