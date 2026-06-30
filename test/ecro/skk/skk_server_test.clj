(ns ecro.skk.skk-server-test
  (:require
    [clojure.test :refer :all]
    [ecro.skk.skk-server :as skk-server]))


(deftest test-parse-response-success
  (testing "parses successful server response"
    (let [resp (.getBytes "1/日本/二本/\n" "EUC-JP")
          cands (skk-server/parse-response resp "EUC-JP")]
      (is (= ["日本" "二本"] cands)))))


(deftest test-parse-response-not-found
  (testing "returns nil for not-found response"
    (let [resp (.getBytes "4みせ " "EUC-JP")]
      (is (nil? (skk-server/parse-response resp "EUC-JP"))))))


(deftest test-parse-response-with-annotations
  (testing "strips annotations from candidates"
    (let [resp (.getBytes "1/日本;にっぽん/二本/\n" "EUC-JP")
          cands (skk-server/parse-response resp "EUC-JP")]
      (is (= ["日本" "二本"] cands)))))


(deftest test-build-request-okuri-nasi
  (testing "builds okuri-nasi request"
    (let [req (skk-server/build-request "にほん" nil "EUC-JP")]
      (is (= "1にほん " (String. req "EUC-JP"))))))


(deftest test-build-request-okuri-ari
  (testing "builds okuri-ari request"
    (let [req (skk-server/build-request "つよ" "i" "EUC-JP")]
      (is (= "2つよ i " (String. req "EUC-JP"))))))
