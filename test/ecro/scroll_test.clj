(ns ecro.scroll-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.scroll :as scroll]))


(deftest test-scroll-down
  (testing "scrolling down moves view down"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 0)
                  (scroll/scroll-down 1))]
      (is (= 1 (:scroll-line buf))))))


(deftest test-scroll-up
  (testing "scrolling up moves view up"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 5)
                  (scroll/scroll-up 1))]
      (is (= 4 (:scroll-line buf))))))


(deftest test-scroll-up-at-top
  (testing "scrolling up at top stays at 0"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 0)
                  (scroll/scroll-up 1))]
      (is (= 0 (:scroll-line buf))))))


(deftest test-scroll-up-command
  (testing "C-v scrolls down by window height"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 0)
                  (scroll/scroll-up-command 5))]
      (is (= 5 (:scroll-line buf))))))


(deftest test-scroll-down-command
  (testing "M-v scrolls up by window height"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 10)
                  (scroll/scroll-down-command 5))]
      (is (= 5 (:scroll-line buf)))))
  (testing "M-v stops at top of buffer"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 2)
                  (scroll/scroll-down-command 5))]
      (is (= 0 (:scroll-line buf))))))


(deftest test-scroll-to-point
  (testing "scroll to keep point visible"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :scroll-line 0)
                  (assoc :text "1\n2\n3\n4\n5\n6\n7\n8\n9\n10")
                  (assoc :point 15))]
      ;; point=15 is on line 7, with height 5, scroll-line should be 3
      (let [adjusted (scroll/adjust-scroll-for-point buf 5)]
        (is (= 3 (:scroll-line adjusted)))))))
