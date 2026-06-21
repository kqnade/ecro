(ns ecro.window-tree-test
  (:require
    [clojure.test :refer :all]
    [ecro.window-tree :as wt]))


(deftest test-remove-window
  (testing "removing a leaf from a two-window container leaves the other window"
    (let [w1 {:type :window :name "left"}
          w2 {:type :window :name "right"}
          container {:type :container :direction :vertical :children [w1 w2]}]
      (is (= w1 (wt/remove-window container w2)))
      (is (= w2 (wt/remove-window container w1)))))
  (testing "removing the last leaf returns nil"
    (let [w {:type :window :name "only"}]
      (is (nil? (wt/remove-window w w)))))
  (testing "removing a nested leaf collapses intermediate containers"
    (let [w1 {:type :window :name "one"}
          w2 {:type :window :name "two"}
          w3 {:type :window :name "three"}
          inner {:type :container :direction :horizontal :children [w1 w2]}
          outer {:type :container :direction :vertical :children [inner w3]}]
      (is (= 2 (count (:children (wt/remove-window outer w1))))))))


(deftest test-collect-windows
  (testing "collecting leaf windows from a tree"
    (let [leaf1 {:type :window :buffer "buf1"}
          leaf2 {:type :window :buffer "buf2"}
          leaf3 {:type :window :buffer "buf3"}
          container {:type :container
                     :direction :vertical
                     :children [leaf1 leaf2]}
          nested {:type :container
                  :direction :horizontal
                  :children [container leaf3]}]
      (is (= [leaf1] (wt/collect-windows leaf1)))
      (is (= [leaf1 leaf2] (wt/collect-windows container)))
      (is (= [leaf1 leaf2 leaf3] (wt/collect-windows nested))))))


(deftest test-collect-empty-container
  (testing "collecting from empty container returns empty"
    (is (= [] (wt/collect-windows {:type :container
                                   :direction :vertical
                                   :children []})))))
