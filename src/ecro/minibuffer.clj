(ns ecro.minibuffer
  (:require [ecro.buffer :as b]))

(defn make-minibuffer
  "Create a new minibuffer."
  []
  {:type :minibuffer
   :buffer (b/make-buffer "*minibuffer*")
   :prompt ""
   :result nil})

(defn set-prompt
  "Set the prompt for the minibuffer."
  [mb prompt]
  (assoc mb :prompt prompt))

(defn insert-char
  "Insert a character into the minibuffer."
  [mb ch]
  (update mb :buffer b/insert-char ch))

(defn complete
  "Complete the minibuffer input and return the result."
  [mb]
  (let [input (:text (:buffer mb))]
    (assoc mb
           :buffer (b/make-buffer "*minibuffer*")
           :result input)))

(defn cancel
  "Cancel the minibuffer input."
  [mb]
  (assoc mb
         :buffer (b/make-buffer "*minibuffer*")
         :result :canceled))

;; Command registry

(defn register-command
  "Register a command in the command registry."
  [registry cmd-name fn]
  (swap! registry assoc cmd-name fn))

(defn execute-command
  "Execute a command from the registry with args."
  [registry cmd-name args]
  (when-let [cmd-fn (get @registry cmd-name)]
    (apply cmd-fn args)))

;; M-x functionality

(defn mx-execute
  "Execute M-x command from minibuffer input."
  [mb registry]
  (let [input (:text (:buffer mb))
        cmd-name (keyword input)]
    (if (contains? @registry cmd-name)
      (do
        (execute-command registry cmd-name [])
        (assoc mb :result input))
      (assoc mb :result :unknown-command))))
