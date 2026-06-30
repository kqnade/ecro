(ns ecro.skk.henkan-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.skk.henkan :as henkan]
    [ecro.skk.jisyo :as jisyo]
    [ecro.skk.state :as skk-state]))


(defn- skk-buffer
  []
  (-> (b/make-buffer "test")
      (assoc :minor-modes #{:skk-mode})
      (skk-state/ensure-state)))


(defn- dict-lookup
  "Create a lookup function from a parsed dict map."
  [dict]
  (fn [midashi okuri-char]
    (if okuri-char
      (jisyo/candidates dict midashi okuri-char)
      (jisyo/candidates dict midashi))))


(deftest test-start-conversion
  (testing "SPC starts conversion and replaces midashi with first candidate"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/set-henkan-key "にほん")
                  (henkan/start (dict-lookup dict)))]
      (is (= "日本" (:text buf)))
      (is (= 2 (:point buf)))
      (is (skk-state/active-conversion? buf))
      (is (= ["日本" "二本"] (skk-state/candidates buf))))))


(deftest test-cycle-candidates
  (testing "cycle-next replaces with next candidate"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/set-henkan-key "にほん")
                  (henkan/start (dict-lookup dict))
                  (henkan/cycle-next))]
      (is (= "二本" (:text buf)))
      (is (= 1 (skk-state/candidate-index buf))))))


(deftest test-confirm-conversion
  (testing "confirm clears henkan state and keeps candidate"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/set-henkan-key "にほん")
                  (henkan/start (dict-lookup dict))
                  (henkan/confirm))]
      (is (= "日本" (:text buf)))
      (is (nil? (skk-state/henkan-mode buf))))))


(deftest test-cancel-active-conversion
  (testing "cancel restores original kana"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/set-henkan-key "にほん")
                  (henkan/start (dict-lookup dict))
                  (henkan/cancel))]
      (is (= "にほん" (:text buf)))
      (is (nil? (skk-state/henkan-mode buf))))))


(deftest test-cancel-henkan-on
  (testing "cancel in henkan-on keeps kana text"
    (let [buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/cancel))]
      (is (= "にほん" (:text buf)))
      (is (nil? (skk-state/henkan-mode buf))))))


(deftest test-no-candidates-notification
  (testing "start conversion with no candidates warns"
    (let [lookup (constantly [])
          buf (-> (skk-buffer)
                  (assoc :text "みせ" :point 2)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/start lookup))]
      (is (= "みせ" (:text buf)))
      (is (= :warn (get-in buf [:notification :level]))))))


(deftest test-okuri-ari-lookup
  (testing "lookup uses midashi + okuri-char for okuri-ari entries"
    (let [dict {:okuri-ari {"つよi" ["強い"]}
                :okuri-nasi {"つよ" ["強" "強さ"]}}
          buf (-> (skk-buffer)
                  (assoc :text "つよい" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (assoc-in [:skk :okuri-char] "i")
                  (henkan/set-henkan-key "つよい")
                  (henkan/start (dict-lookup dict)))]
      (is (= "強い" (:text buf)))
      (is (skk-state/active-conversion? buf)))))


(deftest test-confirm-updates-personal-dictionary
  (testing "confirm captures selected candidate for dictionary update"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/start (dict-lookup dict))
                  (henkan/cycle-next))
          selected (henkan/selected-candidate buf)
          updated-dict (jisyo/update-candidate-order dict
                                                     (get-in buf [:skk :henkan-key])
                                                     (get-in buf [:skk :okuri-char])
                                                     selected)]
      (is (= "二本" selected))
      (is (= ["二本" "日本"] (get-in updated-dict [:okuri-nasi "にほん"]))))))


(deftest test-save-confirmed-candidate
  (testing "saving updated dictionary preserves candidate order"
    (let [tmp-file (str (System/getProperty "java.io.tmpdir") "/ecro_skk_confirm_jisyo")
          dict {:okuri-ari {} :okuri-nasi {"にほん" ["日本" "二本"]}}
          updated (jisyo/update-candidate-order dict "にほん" nil "二本")]
      (try
        (jisyo/save tmp-file updated)
        (is (= ["二本" "日本"] (get-in (jisyo/parse (slurp tmp-file)) [:okuri-nasi "にほん"])))
        (finally
          (io/delete-file tmp-file true)
          (io/delete-file (str tmp-file ".bak") true)
          (io/delete-file (str tmp-file ".tmp") true))))))
