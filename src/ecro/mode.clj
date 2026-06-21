(ns ecro.mode
  (:require
    [clojure.java.io :as io]
    [ecro.keymap :as keymap]))


(def extension->mode
  {".clj" :clojure-mode
   ".cljc" :clojure-mode
   ".cljs" :clojure-mode
   ".md" :markdown-mode
   ".txt" :text-mode})


(def mode-registry
  "Map of major mode keywords to their definitions."
  (atom {:fundamental-mode {:name "Fundamental"
                            :keymap (keymap/make-keymap)}
         :text-mode {:name "Text"
                     :keymap (keymap/make-keymap)
                     :parent :fundamental-mode}}))


(defn registered-modes
  "Return the set of registered major mode keywords."
  []
  (set (keys @mode-registry)))


(defn mode-from-extension
  "Return the major mode for a file extension, or fundamental-mode."
  [ext]
  (get extension->mode (clojure.string/lower-case ext) :fundamental-mode))


(defn- filename-from
  "Extract the filename portion from a buffer name or path."
  [name-or-path]
  (if (nil? name-or-path)
    ""
    (.getName (io/file name-or-path))))


(defn mode-from-buffer-name
  "Infer major mode from a buffer name or file path."
  [name-or-path]
  (let [filename (filename-from name-or-path)
        ext (second (re-find #"(\.[^.]+)$" filename))]
    (mode-from-extension (or ext ""))))


(defn set-buffer-mode
  "Set :mode on a buffer based on its filepath or name, preserving an existing mode."
  [buf]
  (if (:mode buf)
    buf
    (let [source (or (:filepath buf) (:name buf) "")
          mode (mode-from-buffer-name source)]
      (assoc buf :mode mode))))


(defn toggle-minor-mode
  "Toggle a minor mode keyword on a buffer. Returns updated buffer."
  [buf mode-kw]
  (let [modes (set (:minor-modes buf))]
    (assoc buf :minor-modes
           (if (contains? modes mode-kw)
             (disj modes mode-kw)
             (conj modes mode-kw)))))


(defn minor-mode-active?
  "Return true if the given minor mode is active on the buffer."
  [buf mode-kw]
  (contains? (set (:minor-modes buf)) mode-kw))
