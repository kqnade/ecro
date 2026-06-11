(ns ecro.window-tree-test
  (:require
    [clojure.test :refer :all]
    [ecro.window-tree :as wt]))


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
