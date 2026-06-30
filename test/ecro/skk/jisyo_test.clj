(ns ecro.skk.jisyo-test
  (:require
    [clojure.java.io :as io]
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


(deftest test-update-candidate-order
  (testing "selected candidate moves to front"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          updated (jisyo/update-candidate-order dict "にほん" nil "二本")]
      (is (= ["二本" "日本"] (get-in updated [:okuri-nasi "にほん"]))))))


(deftest test-update-candidate-order-okuri-ari
  (testing "okuri-ari entry moves selected candidate to front"
    (let [dict {:okuri-ari {"つよi" ["強い" "強"]}
                :okuri-nasi {}}
          updated (jisyo/update-candidate-order dict "つよ" "i" "強")]
      (is (= ["強" "強い"] (get-in updated [:okuri-ari "つよi"]))))))


(deftest test-update-candidate-order-creates-entry
  (testing "creates new entry when midashi does not exist"
    (let [dict {:okuri-ari {} :okuri-nasi {}}
          updated (jisyo/update-candidate-order dict "あたらしい" nil "新しい")]
      (is (= ["新しい"] (get-in updated [:okuri-nasi "あたらしい"]))))))


(deftest test-save-and-reload
  (testing "saving and reloading preserves dictionary"
    (let [tmp-file (str (System/getProperty "java.io.tmpdir") "/ecro_skk_test_jisyo")
          dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}]
      (try
        (jisyo/save tmp-file dict)
        (is (= dict (jisyo/parse (slurp tmp-file))))
        (finally
          (io/delete-file tmp-file true)
          (io/delete-file (str tmp-file ".bak") true)
          (io/delete-file (str tmp-file ".tmp") true))))))


(deftest test-save-creates-backup
  (testing "save creates backup of existing file"
    (let [tmp-file (str (System/getProperty "java.io.tmpdir") "/ecro_skk_backup_jisyo")
          dict1 {:okuri-ari {} :okuri-nasi {"にほん" ["日本"]}}
          dict2 {:okuri-ari {} :okuri-nasi {"にほん" ["日本" "二本"]}}]
      (try
        (jisyo/save tmp-file dict1)
        (jisyo/save tmp-file dict2)
        (is (= dict1 (jisyo/parse (slurp (str tmp-file ".bak")))))
        (finally
          (io/delete-file tmp-file true)
          (io/delete-file (str tmp-file ".bak") true)
          (io/delete-file (str tmp-file ".tmp") true))))))
