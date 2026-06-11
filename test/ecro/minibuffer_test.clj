(ns ecro.minibuffer-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.minibuffer :as mb]))


(deftest test-make-minibuffer
  (testing "minibuffer creation"
    (let [mb (mb/make-minibuffer)]
      (is (= "" (:text (:buffer mb))))
      (is (= :minibuffer (:type mb))))))


(deftest test-minibuffer-prompt
  (testing "setting prompt on minibuffer"
    (let [mb (-> (mb/make-minibuffer)
                 (mb/set-prompt "M-x "))]
      (is (= "M-x " (:prompt mb))))))


(deftest test-minibuffer-input
  (testing "typing in minibuffer"
    (let [mb (-> (mb/make-minibuffer)
                 (mb/insert-char \f)
                 (mb/insert-char \i)
                 (mb/insert-char \n)
                 (mb/insert-char \d))]
      (is (= "find" (:text (:buffer mb)))))))


(deftest test-minibuffer-complete
  (testing "completing minibuffer input"
    (let [mb (-> (mb/make-minibuffer)
                 (mb/insert-char \f)
                 (mb/insert-char \i)
                 (mb/insert-char \n)
                 (mb/insert-char \d)
                 (mb/complete))]
      (is (= "" (:text (:buffer mb))))
      (is (= "find" (:result mb))))))


(deftest test-minibuffer-cancel
  (testing "canceling minibuffer input"
    (let [mb (-> (mb/make-minibuffer)
                 (mb/insert-char \f)
                 (mb/cancel))]
      (is (= "" (:text (:buffer mb))))
      (is (= :canceled (:result mb))))))


(deftest test-command-registry
  (testing "registering and executing commands"
    (let [registry (atom {})
          _ (mb/register-command registry :test-cmd (fn [] "executed"))]
      (is (contains? @registry :test-cmd))
      (is (= "executed" ((get @registry :test-cmd)))))))


(deftest test-execute-command
  (testing "execute registered command"
    (let [registry (atom {})
          _ (mb/register-command registry :hello (fn [args] (str "Hello " args)))
          result (mb/execute-command registry :hello ["world"])]
      (is (= "Hello world" result)))))


(deftest test-execute-unknown-command
  (testing "execute unknown command returns nil"
    (let [registry (atom {})]
      (is (nil? (mb/execute-command registry :unknown []))))))


(deftest test-minibuffer-mx-executes-command
  (testing "M-x reads minibuffer text and executes command"
    (let [registry (atom {})
          _ (mb/register-command registry :test (fn [] "done"))
          mb (-> (mb/make-minibuffer)
                 (mb/set-prompt "M-x ")
                 (mb/insert-char \t)
                 (mb/insert-char \e)
                 (mb/insert-char \s)
                 (mb/insert-char \t)
                 (mb/mx-execute registry))]
      (is (= "test" (:result mb)))
      (is (= "done" (mb/execute-command registry :test []))))))


(deftest test-minibuffer-mx-unknown-command
  (testing "M-x with unknown command returns :unknown-command"
    (let [registry (atom {})
          mb (-> (mb/make-minibuffer)
                 (mb/insert-char \x)
                 (mb/mx-execute registry))]
      (is (= :unknown-command (:result mb))))))


(deftest test-prompt-for
  (testing "prompt-for creates minibuffer with command"
    (let [mb (mb/prompt-for "Find file: " :open-file)]
      (is (= "Find file: " (:prompt mb)))
      (is (= :open-file (:command mb)))
      (is (= :minibuffer (:type mb))))))
