(ns ecro.notification)


(def levels #{:info :warn :error :debug})


(defn make-notification
  "Create a notification map with a level and message."
  [level message]
  (when-not (contains? levels level)
    (throw (ex-info "Invalid notification level" {:level level})))
  {:level level
   :message message})


(defn notify
  "Attach a notification to editor state."
  [state level message]
  (assoc state :notification (make-notification level message)))


(defn info
  "Attach an info notification to editor state."
  [state message]
  (notify state :info message))


(defn warn
  "Attach a warning notification to editor state."
  [state message]
  (notify state :warn message))


(defn error
  "Attach an error notification to editor state."
  [state message]
  (notify state :error message))


(defn debug
  "Attach a debug notification to editor state."
  [state message]
  (notify state :debug message))


(defn text
  "Return display text for a notification."
  [notification]
  (when notification
    (str (case (:level notification)
           :info "INFO"
           :warn "WARN"
           :error "ERROR"
           :debug "DEBUG")
         ": " (:message notification))))
