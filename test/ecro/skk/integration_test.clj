(ns ecro.skk.integration-test
  (:require
    [clojure.test :refer :all]
    [ecro.bindings :as bindings]
    [ecro.key :as key]
    [ecro.state :as state]))


(defn- initial-state
  ([]
   (state/initial-state bindings/default-keymap))
  ([lookup-fn]
   (assoc (state/initial-state bindings/default-keymap) :skk-lookup-fn lookup-fn)))


(defn- key-event
  "Build a key event map for handle-key."
  [code mods]
  {:type :key :key_code code :modifiers mods})


(deftest test-toggle-skk-mode
  (testing "ESC n toggles SKK mode"
    (let [s1 (key/process-event (initial-state) (key-event 27 0))
          s2 (key/process-event s1 (key-event 110 0))]
      (is (contains? (:minor-modes (:current-buffer s2)) :skk-mode))
      (is (= "SKK enabled" (get-in s2 [:notification :message]))))))


(deftest test-skk-hiragana-input
  (testing "SKK converts romaji to hiragana"
    (let [s0 (initial-state)
          s1 (key/process-event s0 (key-event 27 0))
          s2 (key/process-event s1 (key-event 110 0))
          s3 (key/process-event s2 (key-event 107 0))
          s4 (key/process-event s3 (key-event 97 0))]
      (is (= "か" (:text (:current-buffer s4)))))))


(deftest test-skk-start-conversion
  (testing "uppercase starts conversion and SPC looks up"
    (let [lookup (fn [m o]
                   (when (and (= m "にほん") (nil? o))
                     ["日本" "二本"]))
          s0 (initial-state lookup)
          s1 (key/process-event s0 (key-event 27 0))
          s2 (key/process-event s1 (key-event 110 0))
          s3 (key/process-event s2 (key-event 78 0))    ; N
          s4 (key/process-event s3 (key-event 105 0))   ; i
          s5 (key/process-event s4 (key-event 104 0))   ; h
          s6 (key/process-event s5 (key-event 111 0))   ; o
          s7 (key/process-event s6 (key-event 110 0))   ; n
          s8 (key/process-event s7 (key-event 32 0))]   ; SPC
      (is (= "日本" (:text (:current-buffer s8))))))

  (testing "SPC cycles candidates"
    (let [lookup (fn [m o]
                   (when (and (= m "にほん") (nil? o))
                     ["日本" "二本"]))
          s0 (initial-state lookup)
          s1 (key/process-event s0 (key-event 27 0))
          s2 (key/process-event s1 (key-event 110 0))
          s3 (key/process-event s2 (key-event 78 0))
          s4 (key/process-event s3 (key-event 105 0))
          s5 (key/process-event s4 (key-event 104 0))
          s6 (key/process-event s5 (key-event 111 0))
          s7 (key/process-event s6 (key-event 110 0))
          s8 (key/process-event s7 (key-event 32 0))
          s9 (key/process-event s8 (key-event 32 0))]
      (is (= "二本" (:text (:current-buffer s9)))))))


(deftest test-skk-cancel-conversion
  (testing "C-g cancels active conversion"
    (let [lookup (fn [m o]
                   (when (and (= m "にほん") (nil? o))
                     ["日本" "二本"]))
          s0 (initial-state lookup)
          s1 (key/process-event s0 (key-event 27 0))
          s2 (key/process-event s1 (key-event 110 0))
          s3 (key/process-event s2 (key-event 78 0))
          s4 (key/process-event s3 (key-event 105 0))
          s5 (key/process-event s4 (key-event 104 0))
          s6 (key/process-event s5 (key-event 111 0))
          s7 (key/process-event s6 (key-event 110 0))
          s8 (key/process-event s7 (key-event 32 0))
          s9 (key/process-event s8 (key-event 103 1))]  ; C-g
      (is (= "にほん" (:text (:current-buffer s9)))))))


(deftest test-skk-confirm-conversion
  (testing "C-m confirms active conversion"
    (let [lookup (fn [m o]
                   (when (and (= m "にほん") (nil? o))
                     ["日本" "二本"]))
          s0 (initial-state lookup)
          s1 (key/process-event s0 (key-event 27 0))
          s2 (key/process-event s1 (key-event 110 0))
          s3 (key/process-event s2 (key-event 78 0))
          s4 (key/process-event s3 (key-event 105 0))
          s5 (key/process-event s4 (key-event 104 0))
          s6 (key/process-event s5 (key-event 111 0))
          s7 (key/process-event s6 (key-event 110 0))
          s8 (key/process-event s7 (key-event 32 0))
          s9 (key/process-event s8 (key-event 109 1))]  ; C-m
      (is (= "日本" (:text (:current-buffer s9)))))))
