(ns ecro.native
  (:import
    (com.sun.jna
      Library
      Native
      Pointer
      Structure)))


(gen-class
  :name ecro.native.EcroEvent
  :extends com.sun.jna.Structure
  :prefix "event-"
  :state state
  :init init
  :constructors {[] []}
  :exposes-methods {getFieldOrder superGetFieldOrder})


(defn event-init
  []
  [[] (atom {:event_type 0
             :key_code 0
             :modifiers 0})])


(defn event-getFieldOrder
  [this]
  ["event_type" "key_code" "modifiers"])


(gen-interface
  :name ecro.native.IEcroNative
  :extends [com.sun.jna.Library]
  :methods [[ecro_init [] int]
            [ecro_shutdown [] int]
            [ecro_enable_raw_mode [] int]
            [ecro_disable_raw_mode [] int]
            [ecro_enter_alternate_screen [] int]
            [ecro_leave_alternate_screen [] int]
            [ecro_get_terminal_size [ints ints] int]
            [ecro_poll_event [] com.sun.jna.Pointer]
            [ecro_read_event [] com.sun.jna.Pointer]
            [ecro_free_event [com.sun.jna.Pointer] void]])


(defonce ecro-lib
  (delay
    (try
      (Native/loadLibrary "ecro_core" (Class/forName "ecro.native.IEcroNative"))
      (catch Exception e
        (println "Warning: Could not load ecro_core library:" (.getMessage e))
        nil))))


(defn init
  "Initialize the terminal adapter."
  []
  (when-let [lib @ecro-lib]
    (.ecro_init lib)))


(defn shutdown
  "Shutdown the terminal adapter."
  []
  (when-let [lib @ecro-lib]
    (.ecro_shutdown lib)))


(defn enable-raw-mode
  "Enable raw terminal mode."
  []
  (when-let [lib @ecro-lib]
    (.ecro_enable_raw_mode lib)))


(defn disable-raw-mode
  "Disable raw terminal mode."
  []
  (when-let [lib @ecro-lib]
    (.ecro_disable_raw_mode lib)))


(defn enter-alternate-screen
  "Enter alternate screen buffer."
  []
  (when-let [lib @ecro-lib]
    (.ecro_enter_alternate_screen lib)))


(defn leave-alternate-screen
  "Leave alternate screen buffer."
  []
  (when-let [lib @ecro-lib]
    (.ecro_leave_alternate_screen lib)))


(defn get-terminal-size
  "Get terminal size as [width height]."
  []
  (when-let [lib @ecro-lib]
    (let [width (int-array 1)
          height (int-array 1)]
      (.ecro_get_terminal_size lib width height)
      [(aget width 0) (aget height 0)])))


(defn decode-event-data
  "Decode raw event fields into a Clojure event map."
  [event-type key-code modifiers]
  {:type (case event-type
           1 :key
           2 :resize
           :unknown)
   :key_code key-code
   :modifiers modifiers
   :width key-code
   :height modifiers})


(defn poll-event
  "Poll for a terminal event. Returns nil if no event."
  []
  (when-let [lib @ecro-lib]
    (let [event-ptr (.ecro_poll_event lib)]
      (when-not (= Pointer/NULL event-ptr)
        (try
          (decode-event-data
            (.getInt event-ptr 0)
            (.getInt event-ptr 4)
            (.getInt event-ptr 8))
          (finally
            (.ecro_free_event lib event-ptr)))))))


(defn read-event
  "Block and read a terminal event."
  []
  (when-let [lib @ecro-lib]
    (let [event-ptr (.ecro_read_event lib)]
      (when-not (= Pointer/NULL event-ptr)
        (try
          (decode-event-data
            (.getInt event-ptr 0)
            (.getInt event-ptr 4)
            (.getInt event-ptr 8))
          (finally
            (.ecro_free_event lib event-ptr)))))))
