(ns ecro.skk.input-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.skk.input :as input]
    [ecro.skk.state :as skk-state]))


(defn- skk-buffer
  []
  (-> (b/make-buffer "test")
      (assoc :minor-modes #{:skk-mode})
      (skk-state/ensure-state)))


(deftest test-hiragana-input
  (testing "romaji converts to hiragana"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \k)
                  (input/handle-char \a))]
      (is (= "か" (:text buf)))
      (is (= 1 (:point buf))))))


(deftest test-katakana-toggle
  (testing "q toggles between hiragana and katakana"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \q)
                  (input/handle-char \k)
                  (input/handle-char \a))]
      (is (= "カ" (:text buf))))))


(deftest test-latin-mode
  (testing "l enters latin mode and inserts ascii"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \l)
                  (input/handle-char \a))]
      (is (= "a" (:text buf)))
      (is (= :latin (skk-state/mode buf))))))


(deftest test-latin-to-hiragana
  (testing "C-j returns from latin mode to hiragana"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \l)
                  (input/handle-char \newline)
                  (input/handle-char \k)
                  (input/handle-char \a))]
      (is (= "か" (:text buf)))
      (is (= :hiragana (skk-state/mode buf))))))


(deftest test-pending-prefix
  (testing "incomplete romaji waits as prefix"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \k))]
      (is (= "" (:text buf)))
      (is (= "k" (skk-state/kana-prefix buf))))))


(deftest test-flush-prefix
  (testing "flush-prefix emits pending kana"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \k)
                  (input/flush-prefix))]
      (is (= "k" (:text buf)))
      (is (= "" (skk-state/kana-prefix buf))))))


(deftest test-uppercase-starts-henkan
  (testing "uppercase starts henkan mode and inserts kana"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \B)
                  (input/handle-char \e)
                  (input/handle-char \n)
                  (input/handle-char \r)
                  (input/handle-char \i))]
      (is (= "べんり" (:text buf)))
      (is (skk-state/henkan-on? buf))
      (is (= 0 (skk-state/henkan-start buf))))))


(deftest test-okurigana-input
  (testing "second uppercase marks okuri char"
    (let [buf (-> (skk-buffer)
                  (input/handle-char \T)
                  (input/handle-char \u)
                  (input/handle-char \y)
                  (input/handle-char \o)
                  (input/handle-char \I))]
      (is (= "つよい" (:text buf)))
      (is (skk-state/henkan-on? buf))
      (is (= "i" (get-in buf [:skk :okuri-char]))))))
