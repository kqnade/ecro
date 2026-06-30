(ns ecro.skk.kana-test
  (:require
    [clojure.test :refer :all]
    [ecro.skk.kana :as kana]))


(deftest test-step-simple
  (testing "single character inputs emit kana immediately"
    (is (= {:state :emit :kana "あ" :prefix ""} (kana/step kana/base-tree "a")))
    (is (= {:state :emit :kana "か" :prefix ""} (kana/step kana/base-tree "k" "a")))))


(deftest test-step-wait
  (testing "prefixes that can continue wait"
    (is (= {:state :wait :prefix "k"} (kana/step kana/base-tree "k")))
    (is (= {:state :wait :prefix "s"} (kana/step kana/base-tree "s")))
    (is (= {:state :wait :prefix "sh"} (kana/step kana/base-tree "s" "h")))))


(deftest test-step-doubled-consonant
  (testing "doubled consonants emit sokuon and keep continuation"
    (is (= {:state :emit :kana "っ" :prefix "k"} (kana/step kana/base-tree "k" "k")))))


(deftest test-step-n
  (testing "n alone waits for next input"
    (is (= {:state :wait :prefix "n"} (kana/step kana/base-tree "n")))))


(deftest test-step-na
  (testing "na follows n as a child rule"
    (is (= {:state :emit :kana "な" :prefix ""} (kana/step kana/base-tree "n" "a")))))


(deftest test-make-tree-with-user-rules
  (testing "user rules override base rules"
    (let [tree (kana/make-tree [["z" nil ["ゼット" "ぜっと"]]])]
      (is (= {:state :emit :kana "ぜっと" :prefix ""} (kana/step tree "z"))))))


(deftest test-step-kana-mode
  (testing "katakana step emits katakana side"
    (is (= {:state :emit :kana "ア" :prefix ""} (kana/step-katakana kana/base-tree "a")))
    (is (= {:state :emit :kana "カ" :prefix ""} (kana/step-katakana kana/base-tree "k" "a")))))


(deftest test-flush-prefix
  (testing "flush emits pending kana and clears prefix"
    (is (= ["か" ""] (kana/flush-prefix kana/base-tree "ka")))
    (is (= [nil "k"] (kana/flush-prefix kana/base-tree "k")))))


(deftest test-step-noop
  (testing "unknown input returns noop"
    (is (= {:state :noop :prefix ""} (kana/step kana/base-tree "q")))
    (is (= {:state :noop :prefix ""} (kana/step kana/base-tree "x" "q")))))


(deftest test-step-retry
  (testing "when prefix cannot continue but prefix itself emits, retry the char"
    (let [result (kana/step kana/base-tree "ka" "z")]
      (is (= :emit (:state result)))
      (is (= "か" (:kana result)))
      (is (= "z" (:retry result))))))
