(ns ecro.file
  (:require
    [clojure.java.io :as io]
    [ecro.buffer :as b])
  (:import
    (java.nio.file
      CopyOption
      Files
      LinkOption
      StandardCopyOption)
    (java.nio.file.attribute
      FileAttribute
      PosixFileAttributeView)))


(defn- preserve-posix-permissions
  [source target]
  (when (Files/exists source (make-array LinkOption 0))
    (when-let [attribute-view (Files/getFileAttributeView source
                                                          PosixFileAttributeView
                                                          (make-array LinkOption 0))]
      (Files/setPosixFilePermissions target
                                     (.permissions (.readAttributes attribute-view))))))


(defn- atomic-spit
  [filepath text]
  (let [target (-> filepath io/file .toPath .toAbsolutePath)
        temp-file (Files/createTempFile (.getParent target)
                                        (str "." (.getFileName target) ".")
                                        ".tmp"
                                        (make-array FileAttribute 0))]
    (try
      (preserve-posix-permissions target temp-file)
      (spit (.toFile temp-file) text)
      (Files/move temp-file
                  target
                  (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                          StandardCopyOption/REPLACE_EXISTING]))
      (finally
        (Files/deleteIfExists temp-file)))))


(defn read-file
  "Read a file into a buffer. Returns empty buffer if file doesn't exist."
  [filepath]
  (let [file (io/file filepath)]
    (if (.exists file)
      (let [text (slurp file)]
        (assoc (b/make-buffer (.getName file))
               :text text
               :saved-text text
               :filepath filepath))
      (assoc (b/make-buffer (.getName file))
             :text ""
             :filepath filepath))))


(defn write-file
  "Write buffer content to its filepath. Returns nil if no filepath."
  [buf]
  (when-let [filepath (:filepath buf)]
    (atomic-spit filepath (:text buf))
    buf))


(defn find-file
  "Command: find-file (C-x C-f). Read a file into a new buffer."
  [filepath]
  (read-file filepath))


(defn save-buffer
  "Command: save-buffer (C-x C-s). Write current buffer to file."
  [buf]
  (write-file buf))


(defn write-file-as
  "Write buffer content to a new filepath and update buffer's filepath and name."
  [buf filepath]
  (spit filepath (:text buf))
  (assoc buf
         :filepath filepath
         :name (.getName (io/file filepath))
         :saved-text (:text buf)))
