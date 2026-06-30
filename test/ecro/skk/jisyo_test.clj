(ns ecro.skk.jisyo-test
  (:require
    [clojure.test :refer :all]
    [ecro.skk.jisyo :as jisyo]))


(deftest test-parse-empty
  (testing "empty dictionary parses to empty maps"
    (is (= {:okuri-ari {} :okuri-nasi {}} (jisyo/parse "")))
    (is (= {:okuri-ari {} :okuri-nasi {}} (jisyo/parse "\n\n")))))


(deftest test-parse-comments-and-headers
  (testing "comment lines and headers are ignored"
    (let [text ";; okuri-ari entries.\n;; comment\nかk /書/描/\n;; okuri-nasi entries.\n;; another comment\nかな /仮名/かな/"
          result (jisyo/parse text)]
      (is (= {"かk" ["書" "描"]} (:okuri-ari result)))
      (is (= {"かな" ["仮名" "かな"]} (:okuri-nasi result))))))


(deftest test-parse-candidate-order
  (testing "candidate order is preserved"
    (let [text ";; okuri-nasi entries.\nにほん /日本/二本/"
          result (jisyo/parse text)]
      (is (= ["日本" "二本"] (get-in result [:okuri-nasi "にほん"]))))))


(deftest test-parse-annotations
  (testing "annotations after semicolon are stripped"
    (let [text ";; okuri-nasi entries.\nかな /仮名;annotation/かな/"
          result (jisyo/parse text)]
      (is (= ["仮名" "かな"] (get-in result [:okuri-nasi "かな"]))))))


(deftest test-candidates-okuri-nasi
  (testing "lookup okuri-nasi section"
    (let [dict {:okuri-ari {"かk" ["書"]}
                :okuri-nasi {"にほん" ["日本" "二本"]}}]
      (is (= ["日本" "二本"] (jisyo/candidates dict "にほん")))
      (is (= [] (jisyo/candidates dict "みせ"))))))


(deftest test-candidates-okuri-ari
  (testing "lookup okuri-ari section with okuri char"
    (let [dict {:okuri-ari {"つよi" ["強" "強い"]}
                :okuri-nasi {"つよ" ["強" "強さ"]}}]
      (is (= ["強" "強い"] (jisyo/candidates dict "つよ" "i")))
      (is (= ["強" "強さ"] (jisyo/candidates dict "つよ"))))))
