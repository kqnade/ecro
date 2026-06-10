(ns ecro.window-test
  (:require [clojure.test :refer :all]
            [ecro.window :as w]
            [ecro.buffer :as b]))

(deftest test-create-window
  (testing "window creation with buffer"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf)]
      (is (= buf (:buffer win)))
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
      (is (= win (:root-window frame))))))

(deftest test-vertical-split
  (testing "splitting window vertically creates two windows side by side"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf 80 24)
          frame (w/make-frame win)
          new-frame (w/split-window-vertical frame (:root-window frame))]
      (is (= 2 (count (w/get-windows new-frame))))
      (is (= 80 (:width (:root-window new-frame))))
      (is (= 40 (:width (first (:children (:root-window new-frame)))))))))

(deftest test-horizontal-split
  (testing "splitting window horizontally creates two windows stacked"
    (let [buf (b/make-buffer "test")
          win (w/make-window buf 80 24)
          frame (w/make-frame win)
          new-frame (w/split-window-horizontal frame (:root-window frame))]
      (is (= 2 (count (w/get-windows new-frame))))
      (is (= 24 (:height (:root-window new-frame))))
      (is (= 12 (:height (first (:children (:root-window new-frame)))))))))

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

(deftest test-window-buffer-assignment
  (testing "assigning different buffers to windows"
    (let [buf1 (b/make-buffer "buffer1")
          buf2 (b/make-buffer "buffer2")
          win1 (w/make-window buf1)
          win2 (w/make-window buf2)]
      (is (= "buffer1" (:name (:buffer win1))))
      (is (= "buffer2" (:name (:buffer win2)))))))

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
