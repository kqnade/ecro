(ns ecro.mode
  (:require
    [clojure.java.io :as io]))


(def extension->mode
  {".clj" :clojure-mode
   ".cljc" :clojure-mode
   ".cljs" :clojure-mode
   ".md" :markdown-mode
   ".txt" :text-mode})


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
