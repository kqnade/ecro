(ns ecro.file
  (:require
    [clojure.java.io :as io]
    [ecro.buffer :as b])
  (:import
    (java.nio.file
      CopyOption
      FileSystemLoopException
      Files
      LinkOption
      StandardCopyOption)
    (java.nio.file.attribute
      AclFileAttributeView
      FileAttribute
      PosixFileAttributeView
      PosixFilePermissions)
    (java.util
      UUID)))


(defn- file-attribute-view
  [path attribute-view-class]
  (Files/getFileAttributeView path
                              attribute-view-class
                              (make-array LinkOption 0)))


(defn- posix-attribute-view
  [path]
  (file-attribute-view path PosixFileAttributeView))


(defn- acl-attribute-view
  [path]
  (file-attribute-view path AclFileAttributeView))


(defn- preserve-posix-attributes
  [source target]
  (when (Files/exists source (make-array LinkOption 0))
    (when-let [source-view (posix-attribute-view source)]
      (when-let [target-view (posix-attribute-view target)]
        (let [attributes (.readAttributes source-view)]
          (.setGroup target-view (.group attributes))
          (.setPermissions target-view (.permissions attributes))
          (.setOwner target-view (.owner attributes)))))))


(defn- preserve-acl-attributes
  [source target]
  (when (Files/exists source (make-array LinkOption 0))
    (when-let [source-view (acl-attribute-view source)]
      (when-let [target-view (acl-attribute-view target)]
        (.setAcl target-view (.getAcl source-view))
        (.setOwner target-view (.getOwner source-view))))))


(defn- initial-temp-file-attributes
  [target]
  (if (Files/exists target (make-array LinkOption 0))
    (if-let [attribute-view (posix-attribute-view target)]
      (into-array FileAttribute
                  [(PosixFilePermissions/asFileAttribute
                     (.permissions (.readAttributes attribute-view)))])
      (make-array FileAttribute 0))
    (make-array FileAttribute 0)))


(defn- create-save-temp-file
  [target]
  (Files/createFile (.resolve (.getParent target)
                              (str ".ecro-"
                                   (UUID/randomUUID)
                                   ".tmp"))
                    (initial-temp-file-attributes target)))


(defn- resolve-save-target
  [filepath]
  (loop [target (-> filepath io/file .toPath .toAbsolutePath .normalize)
         seen #{}]
    (if (Files/isSymbolicLink target)
      (if (contains? seen target)
        (throw (FileSystemLoopException. (str target)))
        (let [referent (Files/readSymbolicLink target)
              resolved (if (.isAbsolute referent)
                         referent
                         (.resolve (.getParent target) referent))]
          (recur (.normalize resolved) (conj seen target))))
      target)))


(defn- move-file-atomically
  "Replace target atomically or throw; never fall back to a non-atomic move."
  [source target]
  (Files/move source
              target
              (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                      StandardCopyOption/REPLACE_EXISTING])))


(defn- atomic-spit
  [filepath text]
  (let [target (resolve-save-target filepath)
        temp-file (create-save-temp-file target)]
    (try
      (preserve-posix-attributes target temp-file)
      (preserve-acl-attributes target temp-file)
      (spit (.toFile temp-file) text)
      (move-file-atomically temp-file target)
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
  (atomic-spit filepath (:text buf))
  (assoc buf
         :filepath filepath
         :name (.getName (io/file filepath))
         :saved-text (:text buf)))
