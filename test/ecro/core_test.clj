(ns ecro.core-test
  (:require [clojure.test :refer :all]
            [ecro.core :as core]))

(deftest test-jna-available
  (testing "JNA is available on classpath"
    (is (try
          (Class/forName "com.sun.jna.Library")
          true
          (catch ClassNotFoundException _
            false)))))

(deftest test-core-namespace-loads
  (testing "ecro.core namespace loads without errors"
    (is (find-ns 'ecro.core))))
