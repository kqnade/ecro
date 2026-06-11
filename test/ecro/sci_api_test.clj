(ns ecro.sci-api-test
  (:require
    [clojure.test :refer :all]
    [ecro.sci-api :as api]
    [sci.core :as sci]))


(deftest test-theme
  (testing "theme sets :theme in state"
    (let [state (atom {:theme :light})]
      (api/theme state :dark)
      (is (= :dark (:theme @state))))))


(deftest test-map-key
  (testing "map-key adds binding to state's keymap"
    (let [state (atom {:keymap {:bindings {}}})]
      (api/map-key state ["C-x" "C-s"] :save-buffer)
      (is (= :save-buffer (get-in @state [:keymap :bindings "C-x" "C-s"]))))))


(deftest test-command
  (testing "command registers function in state's commands"
    (let [state (atom {:commands {}})
          f (fn [] "hello")]
      (api/command state :hello f)
      (is (= "hello" ((get-in @state [:commands :hello])))))))


(deftest test-message
  (testing "message sets :message in state"
    (let [state (atom {})]
      (api/message state "hello")
      (is (= "hello" (:message @state))))))


(deftest test-sci-bindings
  (testing "sci-bindings exposes ecro API to SCI context"
    (let [state (atom {:theme :light :keymap {:bindings {}} :commands {}})
          namespaces (api/sci-bindings state)
          ctx (sci/init {:namespaces namespaces})]
      (sci/eval-string* ctx "(ecro/theme :dark)")
      (is (= :dark (:theme @state))))))
