(ns ecro.sci-api
  (:require
    [sci.core :as sci]))


(defn theme
  "Set the editor theme."
  [state value]
  (swap! state assoc :theme value))


(defn map-key
  "Define a key binding in the editor keymap."
  [state key-seq cmd]
  (swap! state assoc-in (into [:keymap :bindings] key-seq) cmd))


(defn command
  "Register a command function."
  [state cmd-name f]
  (swap! state assoc-in [:commands cmd-name] f))


(defn message
  "Set a message in the editor state."
  [state msg]
  (swap! state assoc :message msg))


(defn sci-bindings
  "Return SCI namespace bindings for ecro API."
  [state]
  {'ecro {'theme (fn [value] (theme state value))
          'map-key (fn [key-seq cmd] (map-key state key-seq cmd))
          'command (fn [cmd-name f] (command state cmd-name f))
          'message (fn [msg] (message state msg))}})
