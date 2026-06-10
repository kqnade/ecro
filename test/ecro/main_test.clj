(ns ecro.main-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.kill-ring :as kr]
    [ecro.main :as main]))


(deftest test-lead-key-configurable
  (testing "lead-key is configurable"
    (is (= "ESC" @main/lead-key))
    (reset! main/lead-key "C-a")
    (let [km (main/make-keymap)]
      (is (= :find-file (get-in km [:bindings "C-a" "f"]))))
    (reset! main/lead-key "ESC")))


(deftest test-handle-key-esc-prefix
  (testing "ESC starts prefix sequence"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap main/default-keymap
                 :key-sequence []}
          new-state (main/handle-key state 27 0)] ; ESC
      (is (= ["ESC"] (:key-sequence new-state))))))


(deftest test-handle-key-esc-prefix-complete
  (testing "ESC f completes find-file command"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]}
          new-state (main/handle-key state 102 0)] ; f after ESC
      (is (= [] (:key-sequence new-state)))
      (is (not= "test" (:name (:current-buffer new-state)))))))


(deftest test-editor-state-initialized
  (testing "editor state has required keys"
    (let [state @main/editor-state]
      (is (contains? state :running))
      (is (contains? state :key-sequence))
      (is (contains? state :keymap))
      (is (contains? state :current-buffer)))))


(deftest test-kill-line-integration
  (testing "C-k kills line and adds to kill-ring"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \e)
                                     (b/insert-char \l)
                                     (b/insert-char \l)
                                     (b/insert-char \o)
                                     (b/insert-char \space)
                                     (b/insert-char \w)
                                     (b/insert-char \o)
                                     (b/insert-char \r)
                                     (b/insert-char \l)
                                     (b/insert-char \d)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (ecro.kill-ring/make-kill-ring)}
          new-state (main/handle-key state 11 1)] ; Ctrl-k
      (is (= "hello" (:text (:current-buffer new-state))))
      (is (= " world" (ecro.kill-ring/yank (:kill-ring new-state)))))))
