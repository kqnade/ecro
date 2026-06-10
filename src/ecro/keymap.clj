(ns ecro.keymap)


(defn make-keymap
  "Create a new keymap with optional parent keymap."
  ([] {:bindings {} :parent nil})
  ([parent] {:bindings {} :parent parent}))


(defn- build-tree
  "Build a tree from a sequence of keys and a command."
  [tree keys cmd]
  (if (seq keys)
    (let [k (first keys)
          rest-keys (rest keys)]
      (if (seq rest-keys)
        (update tree k (fn [v]
                         (build-tree (or v {}) rest-keys cmd)))
        (assoc tree k cmd)))
    tree))


(defn define-key
  "Define a key binding in the keymap."
  [km keys cmd]
  (update km :bindings build-tree keys cmd))


(defn- lookup-in-tree
  "Look up a key sequence in the tree. Returns:
   - the command if found
   - :prefix if it's a prefix of a defined sequence
   - nil if not found"
  [tree keys]
  (if (seq keys)
    (let [k (first keys)
          rest-keys (rest keys)
          subtree (get tree k)]
      (cond
        (nil? subtree) nil
        (map? subtree) (if (seq rest-keys)
                         (recur subtree rest-keys)
                         :prefix)
        :else subtree))
    nil))


(defn lookup-key
  "Look up a key sequence in the keymap hierarchy."
  [km keys]
  (let [result (lookup-in-tree (:bindings km) keys)]
    (if (nil? result)
      (when (:parent km)
        (recur (:parent km) keys))
      result)))


(defn ctrl-char
  "Convert a character to its control character equivalent."
  [ch]
  (- (int ch) 96))
