(ns ecro.mise-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [ecro.mise :as mise]))


(deftest test-normalize-tool-name
  (testing "simple tool names become keywords"
    (is (= :node (mise/normalize-tool-name "node")))
    (is (= :rust (mise/normalize-tool-name "rust"))))
  (testing "version suffixes are stripped"
    (is (= :node (mise/normalize-tool-name "node@20")))
    (is (= :java (mise/normalize-tool-name "java@21"))))
  (testing "backend prefixes are stripped"
    (is (= :ripgrep (mise/normalize-tool-name "ubi:BurntSushi/ripgrep")))
    (is (= :nodejs (mise/normalize-tool-name "asdf:nodejs"))))
  (testing "special characters become kebab separators"
    (is (= :clojure-cli (mise/normalize-tool-name "clojure_cli")))
    (is (= :some-tool (mise/normalize-tool-name "some.tool")))))


(deftest test-normalize-tools
  (testing "returns a sorted set of normalized keywords"
    (is (= #{:java :node :rust} (mise/normalize-tools ["node" "rust" "java@21"]))))
  (testing "empty input yields empty set"
    (is (= #{} (mise/normalize-tools [])))))


(deftest test-detect-project-tools
  (testing "detects tools when mise.toml exists"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_detect_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")]
      (.mkdirs project)
      (spit (io/file project "mise.toml") "[tools]\nnode = \"20\"\nrust = \"1.70\"\n")
      (let [result (mise/detect-project-tools (io/file project "src" "foo.clj") home)]
        (is (= (.getAbsolutePath (io/file project "mise.toml")) (:mise-path result)))
        (is (= #{:node :rust} (:tools result)))
        (is (vector? (:analyzers result)))
        (is (vector? (:lsps result))))
      (.delete (io/file project "mise.toml"))
      (.delete project)
      (.delete home)
      (.delete tmp))
    (testing "returns empty result when mise.toml is missing"
      (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_detect_" (System/currentTimeMillis)))
            home (io/file tmp "home")
            project (io/file home "project")]
        (.mkdirs project)
        (let [result (mise/detect-project-tools (io/file project "foo.clj") home)]
          (is (nil? (:mise-path result)))
          (is (= #{} (:tools result)))
          (is (vector? (:analyzers result)))
          (is (vector? (:lsps result))))
        (.delete project)
        (.delete home)
        (.delete tmp)))))


(deftest test-cache
  (testing "cached result is returned on second call"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_cache_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")]
      (.mkdirs project)
      (spit (io/file project "mise.toml") "[tools]\nnode = \"20\"\n")
      (mise/clear-cache!)
      (let [first-result (mise/detect-project-tools-cached (io/file project "src" "foo.clj") home)
            second-result (mise/detect-project-tools-cached (io/file project "src" "bar.clj") home)]
        (is (= first-result second-result))
        (is (= #{:node} (:tools second-result))))
      (.delete (io/file project "mise.toml"))
      (.delete project)
      (.delete home)
      (.delete tmp)
      (mise/clear-cache!))))


(deftest test-infer-candidates
  (testing "infers candidates from known tools"
    (is (= {:analyzers [:clj-kondo] :lsps [:clojure-lsp]}
           (mise/infer-candidates #{:clojure}))))
  (testing "merges candidates from multiple tools without duplicates"
    (is (= {:analyzers [:clj-kondo] :lsps [:clojure-lsp]}
           (mise/infer-candidates #{:clojure :clojure-cli}))))
  (testing "returns empty candidates for unknown tools"
    (is (= {:analyzers [] :lsps []}
           (mise/infer-candidates #{:unknown-tool}))))
  (testing "infers rust-analyzer for rust"
    (is (= {:analyzers [:rust-analyzer] :lsps [:rust-analyzer]}
           (mise/infer-candidates #{:rust})))))


(deftest test-parse-tools
  (testing "parses simple tool declarations"
    (is (= ["node" "rust"] (mise/parse-tools "[tools]\nnode = \"20\"\nrust = \"1.70\""))))
  (testing "parses quoted tool names"
    (is (= ["ubi:BurntSushi/ripgrep"] (mise/parse-tools "[tools]\n\"ubi:BurntSushi/ripgrep\" = \"latest\""))))
  (testing "parses table-form tool declarations"
    (is (= ["java"] (mise/parse-tools "[tools]\njava = { version = \"21\" }"))))
  (testing "ignores comments and blank lines"
    (is (= ["node"] (mise/parse-tools "[tools]\n# comment\nnode = \"20\"\n\n"))))
  (testing "stops at next section"
    (let [content "[tools]\nnode = \"20\"\n\n[env]\nFOO = \"bar\""]
      (is (= ["node"] (mise/parse-tools content)))))
  (testing "returns nil when there is no tools section"
    (is (nil? (mise/parse-tools "[env]\nFOO = \"bar\""))))
  (testing "returns empty vector for empty tools section"
    (is (= [] (mise/parse-tools "[tools]\n")))))


(deftest test-load-tools
  (testing "loads tools from a mise.toml file"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_parse_" (System/currentTimeMillis)))]
      (.mkdirs tmp)
      (let [mise-path (io/file tmp "mise.toml")]
        (spit mise-path "[tools]\nnode = \"20\"\nrust = \"1.70\"\n")
        (is (= ["node" "rust"] (mise/load-tools (.getAbsolutePath mise-path))))
        (.delete mise-path)
        (.delete tmp))))
  (testing "returns nil for missing file"
    (is (nil? (mise/load-tools "/tmp/nonexistent_mise_file.toml")))))


(deftest test-find-mise-toml
  (testing "finds mise.toml in the same directory as the file"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")]
      (.mkdirs project)
      (spit (io/file project "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file project "src" "foo.clj") home)]
        (is (= (.getAbsolutePath (io/file project "mise.toml")) result))
        (.delete (io/file project "mise.toml"))
        (.delete project)
        (.delete home)
        (.delete tmp))))
  (testing "finds mise.toml in a parent directory"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")
          sub (io/file project "src")]
      (.mkdirs sub)
      (spit (io/file project "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file sub "foo.clj") home)]
        (is (= (.getAbsolutePath (io/file project "mise.toml")) result))
        (.delete (io/file project "mise.toml"))
        (.delete sub)
        (.delete project)
        (.delete home)
        (.delete tmp))))
  (testing "stops at git root"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          outer (io/file home "outer")
          inner (io/file outer "inner")
          project (io/file inner "project")]
      (.mkdirs project)
      (.mkdirs (io/file inner ".git"))
      (spit (io/file outer "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file project "foo.clj") home)]
        (is (nil? result))
        (.delete (io/file outer "mise.toml"))
        (.delete (io/file inner ".git"))
        (.delete project)
        (.delete inner)
        (.delete outer)
        (.delete home)
        (.delete tmp))))
  (testing "stops at HOME"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")
          parent (io/file tmp "above")]
      (.mkdirs project)
      (.mkdirs parent)
      (spit (io/file parent "mise.toml") "[tools]\n")
      (let [result (mise/find-mise-toml (io/file project "foo.clj") home)]
        (is (nil? result))
        (.delete (io/file parent "mise.toml"))
        (.delete project)
        (.delete parent)
        (.delete home)
        (.delete tmp))))
  (testing "returns nil when no mise.toml exists"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir") (str "ecro_mise_test_" (System/currentTimeMillis)))
          home (io/file tmp "home")
          project (io/file home "project")]
      (.mkdirs project)
      (let [result (mise/find-mise-toml (io/file project "foo.clj") home)]
        (is (nil? result))
        (.delete project)
        (.delete home)
        (.delete tmp)))))
