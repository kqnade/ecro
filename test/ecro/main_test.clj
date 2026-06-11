(ns ecro.main-test
  (:require
    [clojure.java.io :as io]
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


(deftest test-esc-s-unnamed-buffer-keeps-current-buffer
  (testing "ESC s on an unnamed buffer does not replace current buffer with nil"
    (let [buf (b/make-buffer "*scratch*")
          state {:current-buffer buf
                 :buffers [buf]
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]
                 :kill-ring (kr/make-kill-ring)}
          new-state (main/handle-key state 115 0)]
      (is (= buf (:current-buffer new-state)))
      (is (= [buf] (:buffers new-state)))
      (is (= [] (:key-sequence new-state))))))


(deftest test-esc-s-unnamed-buffer-notifies
  (testing "ESC s on an unnamed buffer notifies that there is no file to save"
    (let [buf (b/make-buffer "*scratch*")
          state {:current-buffer buf
                 :buffers [buf]
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]
                 :kill-ring (kr/make-kill-ring)
                 :message nil}
          new-state (main/handle-key state 115 0)]
      (is (= {:level :warn :message "No file to save"}
             (:notification new-state))))))


(deftest test-esc-s-file-buffer-notifies-written
  (testing "ESC s on a file buffer writes and notifies"
    (let [test-file (str (System/getProperty "java.io.tmpdir")
                         "/ecro_save_" (System/currentTimeMillis) ".txt")
          buf (assoc (b/make-buffer "test.txt")
                     :text "saved content"
                     :filepath test-file)
          state {:current-buffer buf
                 :buffers [buf]
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]
                 :kill-ring (kr/make-kill-ring)
                 :message nil}]
      (try
        (let [new-state (main/handle-key state 115 0)]
          (is (= "saved content" (slurp test-file)))
          (is (= {:level :info :message (str "Wrote " test-file)}
                 (:notification new-state)))
          (is (= "saved content" (:saved-text (:current-buffer new-state)))))
        (finally
          (io/delete-file test-file true))))))


(deftest test-esc-s-save-failure-notifies
  (testing "ESC s catches save failures and keeps the buffer"
    (let [dir (io/file (System/getProperty "java.io.tmpdir")
                       (str "ecro_save_dir_" (System/currentTimeMillis)))
          _ (.mkdir dir)
          buf (assoc (b/make-buffer "test.txt")
                     :text "saved content"
                     :filepath (.getPath dir))
          state {:current-buffer buf
                 :buffers [buf]
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]
                 :kill-ring (kr/make-kill-ring)
                 :message nil}]
      (try
        (let [new-state (main/handle-key state 115 0)]
          (is (= buf (:current-buffer new-state)))
          (is (= :error (:level (:notification new-state))))
          (is (clojure.string/starts-with? (:message (:notification new-state))
                                           "Save failed: ")))
        (finally
          (io/delete-file dir true))))))


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
  (testing "C-x kills selected region and deactivates mark"
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
      (is (= "ll" (kr/yank (:kill-ring new-state))))
      (is (nil? (:mark (:current-buffer new-state)))))))


(deftest test-kill-region-reverse-selection-deactivates-mark
  (testing "C-x with reverse selection deactivates mark"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \e)
                                     (b/insert-char \l)
                                     (b/insert-char \l)
                                     (b/insert-char \o)
                                     (b/set-mark)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          new-state (main/handle-key state 24 1)]
      (is (= "he" (:text (:current-buffer new-state))))
      (is (= "llo" (kr/yank (:kill-ring new-state))))
      (is (nil? (:mark (:current-buffer new-state)))))))


(deftest test-kill-region-is-undoable
  (testing "C-x cut can be undone with C-z"
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
          cut (main/handle-key state 24 1)
          undone (main/handle-key cut 26 1)]
      (is (= "heo" (:text (:current-buffer cut))))
      (is (= "hello" (:text (:current-buffer undone))))
      (is (= 4 (:point (:current-buffer undone)))))))


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


(deftest test-yank-is-undoable
  (testing "C-v yank can be undone with C-z"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \i))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (-> (kr/make-kill-ring)
                                (kr/kill-text " there"))}
          yanked (main/handle-key state 22 1)
          undone (main/handle-key yanked 26 1)]
      (is (= "hi there" (:text (:current-buffer yanked))))
      (is (= "hi" (:text (:current-buffer undone))))
      (is (= 2 (:point (:current-buffer undone)))))))


(deftest test-esc-q-quit
  (testing "ESC q quits the editor"
    (let [state {:current-buffer (b/make-buffer "test")
                 :keymap main/default-keymap
                 :key-sequence ["ESC"]
                 :kill-ring (kr/make-kill-ring)
                 :running true}
          new-state (main/handle-key state 113 0)]
      (is (not (:running new-state)))
      (is (= [] (:key-sequence new-state))))))


(deftest test-shift-right-sets-mark-and-extends-region
  (testing "Shift+Right sets mark and extends region"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \e)
                                     (b/insert-char \l)
                                     (b/insert-char \l)
                                     (b/insert-char \o)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          ;; Shift+Right (code 1004, modifiers with Shift=4)
          new-state (main/handle-key state 1004 5)]
      (is (= 2 (:mark (:current-buffer new-state))))
      (is (= 3 (:point (:current-buffer new-state))))
      (is (= "l" (b/region-text (:current-buffer new-state)))))))


(deftest test-plain-movement-deactivates-mark
  (testing "Plain arrow key deactivates mark after Shift+Arrow selection"
    (let [state {:current-buffer (-> (b/make-buffer "test")
                                     (b/insert-char \h)
                                     (b/insert-char \e)
                                     (b/insert-char \l)
                                     (b/insert-char \l)
                                     (b/insert-char \o)
                                     (b/move-point-backward)
                                     (b/move-point-backward)
                                     (b/move-point-backward))
                 :keymap main/default-keymap
                 :key-sequence []
                 :kill-ring (kr/make-kill-ring)}
          selected (main/handle-key state 1004 5)
          moved (main/handle-key selected 1004 0)]
      (is (= 2 (:mark (:current-buffer selected))))
      (is (nil? (:mark (:current-buffer moved))))
      (is (= 4 (:point (:current-buffer moved)))))))


(deftest test-status-line-shows-key-sequence
  (testing "status line displays key sequence without hardcoded C- prefix"
    (let [state {:current-buffer (b/make-buffer "test")
                 :key-sequence ["ESC"]
                 :message nil}]
      (is (clojure.string/includes? (main/status-line state) "  ESC "))))
  (testing "status line displays C-g lead key correctly"
    (let [state {:current-buffer (b/make-buffer "test")
                 :key-sequence ["C-g"]
                 :message nil}]
      (is (clojure.string/includes? (main/status-line state) "  C-g "))))
  (testing "status line omits key sequence when empty"
    (let [state {:current-buffer (b/make-buffer "test")
                 :key-sequence []
                 :message nil}
          line (main/status-line state)]
      (is (not (clojure.string/includes? line "ESC")))
      (is (not (clojure.string/includes? line "C-"))))))


(deftest test-buffer-list-initialized
  (testing "editor state starts with empty buffer list"
    (let [state {:current-buffer (b/make-buffer "*scratch*")
                 :buffers []}]
      (is (= [] (:buffers state))))))
