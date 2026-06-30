(ns ecro.skk.ui-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.skk.state :as skk-state]
    [ecro.skk.ui :as ui]))


(defn- skk-buffer
  []
  (-> (b/make-buffer "test")
      (assoc :minor-modes #{:skk-mode})
      (skk-state/ensure-state)))


(deftest test-status-disabled
  (testing "returns nil when SKK is disabled"
    (is (nil? (ui/status-message (b/make-buffer "test"))))))


(deftest test-status-hiragana
  (testing "shows mode label in hiragana mode"
    (is (= "SKK:かな" (ui/status-message (skk-buffer))))))


(deftest test-status-katakana
  (testing "shows mode label in katakana mode"
    (let [buf (skk-state/set-mode (skk-buffer) :katakana)]
      (is (= "SKK:カナ" (ui/status-message buf))))))


(deftest test-status-henkan-on
  (testing "shows reading midashi while henkan-on"
    (let [buf (-> (skk-buffer)
                  (assoc :text "にほん" :point 3)
                  (skk-state/set-henkan-mode :on)
                  (skk-state/set-henkan-start 0))]
      (is (= "SKK:▽ にほん" (ui/status-message buf))))))


(deftest test-status-active-conversion
  (testing "shows current candidate and index"
    (let [buf (-> (skk-buffer)
                  (assoc :text "日本" :point 2)
                  (skk-state/set-henkan-mode :active)
                  (skk-state/set-henkan-start 0)
                  (skk-state/set-henkan-end 2)
                  (skk-state/set-candidates ["日本" "二本"])
                  (skk-state/set-candidate-index 1))]
      (is (= "SKK:▼ 二本 (2/2)" (ui/status-message buf))))))


(deftest test-candidate-list-message
  (testing "shows all candidates with current marked"
    (let [buf (-> (skk-buffer)
                  (assoc :text "日本" :point 2)
                  (skk-state/set-henkan-mode :active)
                  (skk-state/set-candidates ["日本" "二本"])
                  (skk-state/set-candidate-index 0))]
      (is (= "SKK: ▼日本 □二本" (ui/candidate-list-message buf))))))
