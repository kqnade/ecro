(ns ecro.search-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.search :as search]))


(deftest test-search-forward-found
  (testing "search forward finds match"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :text "hello world"))
          result (search/search-forward buf "world")]
      (is (= 6 (:point result))))))


(deftest test-search-forward-not-found
  (testing "search forward returns nil when not found"
    (let [buf (assoc (b/make-buffer "test") :text "hello world")
          result (search/search-forward buf "xyz")]
      (is (nil? result)))))


(deftest test-search-forward-from-point
  (testing "search forward starts from current point"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :text "foo bar foo")
                  (assoc :point 4))
          result (search/search-forward buf "foo")]
      (is (= 8 (:point result))))))


(deftest test-search-backward-found
  (testing "search backward finds match"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :text "hello world")
                  (assoc :point 11))
          result (search/search-backward buf "hello")]
      (is (= 0 (:point result))))))


(deftest test-search-backward-not-found
  (testing "search backward returns nil when not found"
    (let [buf (assoc (b/make-buffer "test") :text "hello world" :point 11)
          result (search/search-backward buf "xyz")]
      (is (nil? result)))))


(deftest test-incremental-search-state
  (testing "i-search state tracks pattern and direction"
    (let [state (search/make-isearch :forward)]
      (is (= "" (:pattern state)))
      (is (= :forward (:direction state)))
      (is (nil? (:start-point state))))))


(deftest test-isearch-add-char
  (testing "adding char to i-search pattern"
    (let [state (-> (search/make-isearch :forward)
                    (search/isearch-add-char \h))]
      (is (= "h" (:pattern state))))))


(deftest test-isearch-execute-found
  (testing "i-search executes and moves point"
    (let [buf (assoc (b/make-buffer "test") :text "hello world")
          state (-> (search/make-isearch :forward)
                    (assoc :start-point 0)
                    (search/isearch-add-char \w)
                    (search/isearch-add-char \o)
                    (search/isearch-add-char \r)
                    (search/isearch-add-char \l)
                    (search/isearch-add-char \d))
          result (search/isearch-execute state buf)]
      (is (= 6 (:point result))))))


(deftest test-isearch-execute-not-found
  (testing "i-search with no match stays at start"
    (let [buf (assoc (b/make-buffer "test") :text "hello world")
          state (-> (search/make-isearch :forward)
                    (assoc :start-point 0)
                    (search/isearch-add-char \x)
                    (search/isearch-add-char \y)
                    (search/isearch-add-char \z))
          result (search/isearch-execute state buf)]
      (is (= 0 (:point result))))))


(deftest test-isearch-cancel
  (testing "cancel i-search restores original point"
    (let [buf (-> (b/make-buffer "test")
                  (assoc :text "hello world")
                  (assoc :point 5))
          state (-> (search/make-isearch :forward)
                    (assoc :start-point 5)
                    (search/isearch-add-char \x)
                    (search/isearch-add-char \y))
          result (search/isearch-cancel state buf)]
      (is (= 5 (:point result))))))
