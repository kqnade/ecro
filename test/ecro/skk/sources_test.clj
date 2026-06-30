(ns ecro.skk.sources-test
  (:require
    [clojure.test :refer :all]
    [ecro.skk.jisyo :as jisyo]
    [ecro.skk.sources :as sources]))


(deftest test-make-lookup-prefers-file-dict
  (testing "file dictionary is searched first"
    (let [dict {:okuri-ari {} :okuri-nasi {"にほん" ["日本" "二本"]}}
          lookup (sources/make-lookup {:dict dict})]
      (is (= ["日本" "二本"] (lookup "にほん" nil))))))


(deftest test-make-lookup-falls-through
  (testing "lookup returns empty vector when no sources match"
    (let [lookup (sources/make-lookup {:dict {:okuri-ari {} :okuri-nasi {}}})]
      (is (= [] (lookup "みせ" nil))))))


(deftest test-load-file-dict
  (testing "loads and merges personal and large dictionaries"
    (let [dict (sources/load-file-dict {:jisyo-path nil :large-jisyo-path nil})]
      (is (map? dict))
      (is (contains? dict :okuri-ari))
      (is (contains? dict :okuri-nasi)))))
