(ns ecro.main-test
  (:require
    [clojure.test :refer :all]
    [ecro.buffer :as b]
    [ecro.kill-ring :as kr]
    [ecro.main :as main]))


(deftest test-lead-key-configurable
  (testing "lead-key is configurable"
    (is (= "ESC" @main/lead-key))
    (reset! main/lead-key "C-g")
    (let [km (main/make-keymap)]
      (is (= :find-file (get-in km [:bindings "C-g" "f"]))))
    (reset! main/lead-key "ESC")))


(deftest test-default-keymap-editing-bindings
  (testing "C-a and undo bindings are present"
    (is (= :move-beginning-of-line (get-in main/default-keymap [:bindings "C-a"])))
    (is (= :undo (get-in main/default-keymap [:bindings "C-z"])))
    (is (= :redo (get-in main/default-keymap [:bindings "C-S-z"])))
    (is (= :undo (get-in main/default-keymap [:bindings "C-/"])))
    (is (= :undo (get-in main/default-keymap [:bindings "ESC" "u"])))))


(deftest test-key-name-control-shift-and-control-slash
  (testing "terminal key codes map to keymap names"
    (is (= "C-z" (main/key-name 26 1)))
    (is (= "C-S-z" (main/key-name 26 5)))
    (is (= "C-/" (main/key-name 31 1)))))


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


(deftest test-undo-redo-integration
  (testing "C-z undoes and C-S-z redoes buffer edits"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \a))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          undone (main/handle-key state 26 1)
          redone (main/handle-key undone 26 5)]
      (is (= "" (:text (:current-buffer undone))))
      (is (= "a" (:text (:current-buffer redone)))))))


(deftest test-esc-u-undo-integration
  (testing "ESC u undoes buffer edits"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \a))
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]
                 :kill-ring (kr/make-kill-ring)}
          new-state (main/handle-key state 117 0)]
      (is (= "" (:text (:current-buffer new-state))))
      (is (= [] (:key-sequence new-state))))))


(deftest test-control-slash-undo-integration
  (testing "C-/ undoes buffer edits"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \a))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          new-state (main/handle-key state 31 1)]
      (is (= "" (:text (:current-buffer new-state)))))))


(deftest test-kill-region-cut-integration
  (testing "C-x kills selected region"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \e)
                                     (b/insert-char \l)
                                     (b/insert-char \l)
                                     (b/insert-char \o)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/set-mark)
                                     (b/move-point-forward)
                                     (b/move-point-forward))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          new-state (main/handle-key state 24 1)]
      (is (= "heo" (:text (:current-buffer new-state))))
      (is (= "ll" (kr/yank (:kill-ring new-state)))))))


(deftest test-kill-ring-save-copy-integration
  (testing "C-c copies selected region without deleting"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \e)
                                     (b/insert-char \l)
                                     (b/insert-char \l)
                                     (b/insert-char \o)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/set-mark)
                                     (b/move-point-forward)
                                     (b/move-point-forward))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          new-state (main/handle-key state 3 1)]
      (is (= "hello" (:text (:current-buffer new-state))))
      (is (= "ll" (kr/yank (:kill-ring new-state)))))))


(deftest test-yank-paste-integration
  (testing "C-v pastes from kill ring"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \i))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (-> (kr/make-kill-ring)
                                (kr/kill-text " there"))}
          new-state (main/handle-key state 22 1)]
      (is (= "hi there" (:text (:current-buffer new-state)))))))
