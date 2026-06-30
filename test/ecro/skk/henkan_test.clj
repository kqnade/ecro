(ns ecro.skk.henkan-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.skk.henkan :as henkan]
    [ecro.skk.state :as skk-state]))


(defn- skk-buffer
  []
  (-> (b/make-buffer "test")
      (assoc :minor-modes #{:skk-mode})
      (skk-state/ensure-state)))


(deftest test-start-conversion
  (testing "SPC starts conversion and replaces midashi with first candidate"
    (let [dict {:okuri-ari {}
                :okuri-nasi {"にほん" ["日本" "二本"]}}
          buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/set-henkan-key "にほん")
                  (henkan/start dict))]
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
                  (henkan/start dict)
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
                  (henkan/start dict)
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
                  (henkan/start dict)
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
    (let [dict {:okuri-ari {} :okuri-nasi {}}
          buf (-> (skk-buffer)
                  (assoc :text "みせ" :point 2)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0)
                  (henkan/start dict))]
      (is (= "みせ" (:text buf)))
      (is (= :warn (get-in buf [:notification :level]))))))
