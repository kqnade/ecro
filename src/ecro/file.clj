(ns ecro.file
  (:require
    [clojure.java.io :as io]
    [ecro.buffer :as b]))


(defn read-file
  "Read a file into a buffer. Returns empty buffer if file doesn't exist."
  [filepath]
  (let [file (io/file filepath)]
    (if (.exists file)
      (assoc (b/make-buffer (.getName file))
             :text (slurp file)
             :filepath filepath)
      (assoc (b/make-buffer (.getName file))
             :text ""
             :filepath filepath))))


(defn write-file
  "Write buffer content to its filepath. Returns nil if no filepath."
  [buf]
  (when-let [filepath (:filepath buf)]
    (spit filepath (:text buf))
    buf))


(defn find-file
  "Command: find-file (C-x C-f). Read a file into a new buffer."
  [filepath]
  (read-file filepath))


(defn save-buffer
  "Command: save-buffer (C-x C-s). Write current buffer to file."
  [buf]
  (write-file buf))
