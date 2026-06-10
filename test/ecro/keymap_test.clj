(ns ecro.keymap-test
  (:require [clojure.test :refer :all]
            [ecro.keymap :as k]))

(deftest test-empty-keymap
  (testing "empty keymap returns nil for any key"
    (is (nil? (k/lookup-key (k/make-keymap) ["C-f"])))))

(deftest test-define-key
  (testing "defining a simple key binding"
    (let [km (k/define-key (k/make-keymap) ["C-f"] :forward-char)]
      (is (= :forward-char (k/lookup-key km ["C-f"]))))))

(deftest test-define-prefix-key
  (testing "defining a prefix key"
    (let [km (-> (k/make-keymap)
                  (k/define-key ["C-x" "C-f"] :find-file)
                  (k/define-key ["C-x" "C-s"] :save-buffer))]
      (is (= :find-file (k/lookup-key km ["C-x" "C-f"])))
      (is (= :save-buffer (k/lookup-key km ["C-x" "C-s"]))))))

(deftest test-lookup-prefix
  (testing "looking up a prefix returns :prefix if not complete"
    (let [km (k/define-key (k/make-keymap) ["C-x" "C-f"] :find-file)]
      (is (= :prefix (k/lookup-key km ["C-x"]))))))

(deftest test-lookup-undefined-prefix
  (testing "looking up undefined prefix returns nil"
    (let [km (k/make-keymap)]
      (is (nil? (k/lookup-key km ["C-x"]))))))

(deftest test-lookup-complete-sequence
  (testing "looking up complete sequence returns command"
    (let [km (k/define-key (k/make-keymap) ["C-x" "C-f"] :find-file)]
      (is (= :find-file (k/lookup-key km ["C-x" "C-f"]))))))

(deftest test-keymap-inheritance
  (testing "child keymap inherits from parent"
    (let [parent (k/define-key (k/make-keymap) ["C-f"] :forward-char)
          child (k/make-keymap parent)]
      (is (= :forward-char (k/lookup-key child ["C-f"]))))))

(deftest test-child-override-parent
  (testing "child can override parent binding"
    (let [parent (k/define-key (k/make-keymap) ["C-f"] :forward-char)
          child (k/define-key (k/make-keymap parent) ["C-f"] :custom-forward)]
      (is (= :custom-forward (k/lookup-key child ["C-f"]))))))

(deftest test-ctrl-char-conversion
  (testing "converting ctrl characters"
    (is (= 6 (k/ctrl-char \f)))
    (is (= 24 (k/ctrl-char \x)))
    (is (= 3 (k/ctrl-char \c)))))
