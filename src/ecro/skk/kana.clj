(ns ecro.skk.kana
  "Romaji to kana conversion using a DDSKK-style rule tree.

  Rules are vectors of [input next-state [katakana hiragana]]. next-state is
  the continuation prefix when the rule has one, otherwise nil. The tree is
  compiled into nested maps keyed by the next input character.")


(def base-rules
  "Default romaji-to-kana rules inspired by DDSKK's skk-rom-kana-base-rule-list.
  Each rule is [input next-state [katakana hiragana]]."
  [["a" nil ["ア" "あ"]]
   ["bb" "b" ["ッ" "っ"]]
   ["ba" nil ["バ" "ば"]]
   ["be" nil ["ベ" "べ"]]
   ["bi" nil ["ビ" "び"]]
   ["bo" nil ["ボ" "ぼ"]]
   ["bu" nil ["ブ" "ぶ"]]
   ["bya" nil ["ビャ" "びゃ"]]
   ["byo" nil ["ビョ" "びょ"]]
   ["byu" nil ["ビュ" "びゅ"]]
   ["cc" "c" ["ッ" "っ"]]
   ["cha" nil ["チャ" "ちゃ"]]
   ["chi" nil ["チ" "ち"]]
   ["cho" nil ["チョ" "ちょ"]]
   ["chu" nil ["チュ" "ちゅ"]]
   ["dd" "d" ["ッ" "っ"]]
   ["da" nil ["ダ" "だ"]]
   ["de" nil ["デ" "で"]]
   ["dha" nil ["デャ" "でゃ"]]
   ["dhi" nil ["ディ" "でぃ"]]
   ["dho" nil ["デョ" "でょ"]]
   ["dhu" nil ["デュ" "でゅ"]]
   ["di" nil ["ディ" "でぃ"]]
   ["do" nil ["ド" "ど"]]
   ["du" nil ["ドゥ" "どぅ"]]
   ["e" nil ["エ" "え"]]
   ["ff" "f" ["ッ" "っ"]]
   ["fa" nil ["ファ" "ふぁ"]]
   ["fi" nil ["フィ" "ふぃ"]]
   ["fo" nil ["フォ" "ふぉ"]]
   ["fu" nil ["フ" "ふ"]]
   ["fya" nil ["フャ" "ふゃ"]]
   ["fyo" nil ["フョ" "ふょ"]]
   ["fyu" nil ["フュ" "ふゅ"]]
   ["gg" "g" ["ッ" "っ"]]
   ["ga" nil ["ガ" "が"]]
   ["ge" nil ["ゲ" "げ"]]
   ["gi" nil ["ギ" "ぎ"]]
   ["go" nil ["ゴ" "ご"]]
   ["gu" nil ["グ" "ぐ"]]
   ["gya" nil ["ギャ" "ぎゃ"]]
   ["gyo" nil ["ギョ" "ぎょ"]]
   ["gyu" nil ["ギュ" "ぎゅ"]]
   ["ha" nil ["ハ" "は"]]
   ["he" nil ["ヘ" "へ"]]
   ["hi" nil ["ヒ" "ひ"]]
   ["ho" nil ["ホ" "ほ"]]
   ["hu" nil ["フ" "ふ"]]
   ["hya" nil ["ヒャ" "ひゃ"]]
   ["hyo" nil ["ヒョ" "ひょ"]]
   ["hyu" nil ["ヒュ" "ひゅ"]]
   ["i" nil ["イ" "い"]]
   ["jj" "j" ["ッ" "っ"]]
   ["ja" nil ["ジャ" "じゃ"]]
   ["ji" nil ["ジ" "じ"]]
   ["jo" nil ["ジョ" "じょ"]]
   ["ju" nil ["ジュ" "じゅ"]]
   ["jya" nil ["ジャ" "じゃ"]]
   ["jyo" nil ["ジョ" "じょ"]]
   ["jyu" nil ["ジュ" "じゅ"]]
   ["kk" "k" ["ッ" "っ"]]
   ["ka" nil ["カ" "か"]]
   ["ke" nil ["ケ" "け"]]
   ["ki" nil ["キ" "き"]]
   ["ko" nil ["コ" "こ"]]
   ["ku" nil ["ク" "く"]]
   ["kya" nil ["キャ" "きゃ"]]
   ["kyo" nil ["キョ" "きょ"]]
   ["kyu" nil ["キュ" "きゅ"]]
   ["ma" nil ["マ" "ま"]]
   ["me" nil ["メ" "め"]]
   ["mi" nil ["ミ" "み"]]
   ["mo" nil ["モ" "も"]]
   ["mu" nil ["ム" "む"]]
   ["mya" nil ["ミャ" "みゃ"]]
   ["myo" nil ["ミョ" "みょ"]]
   ["myu" nil ["ミュ" "みゅ"]]
   ["n'" nil ["ン" "ん"]]
   ["na" nil ["ナ" "な"]]
   ["ne" nil ["ネ" "ね"]]
   ["ni" nil ["ニ" "に"]]
   ["nn" nil ["ン" "ん"]]
   ["no" nil ["ノ" "の"]]
   ["nu" nil ["ヌ" "ぬ"]]
   ["nya" nil ["ニャ" "にゃ"]]
   ["nyo" nil ["ニョ" "にょ"]]
   ["nyu" nil ["ニュ" "にゅ"]]
   ["o" nil ["オ" "お"]]
   ["pp" "p" ["ッ" "っ"]]
   ["pa" nil ["パ" "ぱ"]]
   ["pe" nil ["ペ" "ぺ"]]
   ["pi" nil ["ピ" "ぴ"]]
   ["po" nil ["ポ" "ぽ"]]
   ["pu" nil ["プ" "ぷ"]]
   ["pya" nil ["ピャ" "ぴゃ"]]
   ["pyo" nil ["ピョ" "ぴょ"]]
   ["pyu" nil ["ピュ" "ぴゅ"]]
   ["rr" "r" ["ッ" "っ"]]
   ["ra" nil ["ラ" "ら"]]
   ["re" nil ["レ" "れ"]]
   ["ri" nil ["リ" "り"]]
   ["ro" nil ["ロ" "ろ"]]
   ["ru" nil ["ル" "る"]]
   ["rya" nil ["リャ" "りゃ"]]
   ["ryo" nil ["リョ" "りょ"]]
   ["ryu" nil ["リュ" "りゅ"]]
   ["ss" "s" ["ッ" "っ"]]
   ["sa" nil ["サ" "さ"]]
   ["se" nil ["セ" "せ"]]
   ["sha" nil ["シャ" "しゃ"]]
   ["shi" nil ["シ" "し"]]
   ["sho" nil ["ショ" "しょ"]]
   ["shu" nil ["シュ" "しゅ"]]
   ["so" nil ["ソ" "そ"]]
   ["su" nil ["ス" "す"]]
   ["sya" nil ["シャ" "しゃ"]]
   ["syo" nil ["ショ" "しょ"]]
   ["syu" nil ["シュ" "しゅ"]]
   ["tt" "t" ["ッ" "っ"]]
   ["ta" nil ["タ" "た"]]
   ["te" nil ["テ" "て"]]
   ["tha" nil ["テャ" "てゃ"]]
   ["thi" nil ["ティ" "てぃ"]]
   ["tho" nil ["テョ" "てょ"]]
   ["thu" nil ["テュ" "てゅ"]]
   ["ti" nil ["ティ" "てぃ"]]
   ["to" nil ["ト" "と"]]
   ["tsu" nil ["ツ" "つ"]]
   ["tu" nil ["ツ" "つ"]]
   ["tya" nil ["チャ" "ちゃ"]]
   ["tyo" nil ["チョ" "ちょ"]]
   ["tyu" nil ["チュ" "ちゅ"]]
   ["u" nil ["ウ" "う"]]
   ["vv" "v" ["ッ" "っ"]]
   ["va" nil ["ヴァ" "う゛ぁ"]]
   ["vi" nil ["ヴィ" "う゛ぃ"]]
   ["vo" nil ["ヴォ" "う゛ぉ"]]
   ["vu" nil ["ヴ" "う゛"]]
   ["ww" "w" ["ッ" "っ"]]
   ["wa" nil ["ワ" "わ"]]
   ["we" nil ["ウェ" "うぇ"]]
   ["wi" nil ["ウィ" "うぃ"]]
   ["wo" nil ["ヲ" "を"]]
   ["wu" nil ["ウ" "う"]]
   ["xx" "x" ["ッ" "っ"]]
   ["xa" nil ["ァ" "ぁ"]]
   ["xe" nil ["ェ" "ぇ"]]
   ["xi" nil ["ィ" "ぃ"]]
   ["xo" nil ["ォ" "ぉ"]]
   ["xtu" nil ["ッ" "っ"]]
   ["xu" nil ["ゥ" "ぅ"]]
   ["xwa" nil ["ヮ" "ゎ"]]
   ["xya" nil ["ャ" "ゃ"]]
   ["xyo" nil ["ョ" "ょ"]]
   ["xyu" nil ["ュ" "ゅ"]]
   ["yy" "y" ["ッ" "っ"]]
   ["ya" nil ["ヤ" "や"]]
   ["yo" nil ["ヨ" "よ"]]
   ["yu" nil ["ユ" "ゆ"]]
   ["zz" "z" ["ッ" "っ"]]
   ["za" nil ["ザ" "ざ"]]
   ["ze" nil ["ゼ" "ぜ"]]
   ["zi" nil ["ジ" "じ"]]
   ["zo" nil ["ゾ" "ぞ"]]
   ["zu" nil ["ズ" "ず"]]
   ["zya" nil ["ジャ" "じゃ"]]
   ["zyo" nil ["ジョ" "じょ"]]
   ["zyu" nil ["ジュ" "じゅ"]]])


(defn compile-rules
  "Compile a rule list into a trie map. Each node is a map of character to
  child node. A leaf stores :output and optional :next keys alongside child
  branches, allowing a prefix to be both a complete rule and a parent of
  longer rules."
  [rules]
  (reduce
    (fn [tree [input next-state output]]
      (let [chars (seq input)
            leaf {:output output :next next-state}]
        (update-in tree chars #(merge (or % {}) leaf))))
    {}
    rules))


(defn make-tree
  "Compile base rules merged with optional user rules.

  User rules are vectors of [input next-state [katakana hiragana]]. They
  override base rules with the same input."
  ([]
   (compile-rules base-rules))
  ([user-rules]
   (compile-rules (concat base-rules user-rules))))


(def base-tree
  "Compiled base rule tree."
  (make-tree))


(defn step
  "Advance one character through the rule tree.

  Returns a map describing the result:
  - {:state :wait :prefix new-prefix} when the prefix can continue
  - {:state :emit :kana kana :prefix next-prefix} when a rule completes
  - {:state :noop :prefix \"\"} when the input cannot be consumed

  The output side selects hiragana or katakana from the rule."
  ([tree ch]
   (step tree "" ch))
  ([tree prefix ch]
   (let [new-prefix (str prefix ch)
         node (get-in tree (seq new-prefix))]
     (cond
       (:output node)
       {:state :emit
        :kana (get-in node [:output 1])
        :prefix (or (:next node) "")}

       (and (map? node) (some #(not (#{:output :next} %)) (keys node)))
       {:state :wait :prefix new-prefix}

       :else
       (if (seq prefix)
         (let [prefix-node (get-in tree (seq prefix))
               prefix-output (:output prefix-node)]
           (cond
             prefix-output
             {:state :emit
              :kana (get-in prefix-output [1])
              :prefix ""
              :retry ch}

             (= prefix "n")
             {:state :emit
              :kana "ん"
              :prefix ""
              :retry ch}

             :else
             {:state :noop :prefix ""}))
         {:state :noop :prefix ""})))))


(defn step-katakana
  "Like step but emits the katakana side of the rule."
  ([tree ch]
   (step-katakana tree "" ch))
  ([tree prefix ch]
   (let [result (step tree prefix ch)
         new-prefix (str prefix ch)]
     (if (:kana result)
       (let [hiragana (:kana result)
             output (or (:output (get-in tree (seq new-prefix)))
                        (:output (get-in tree (seq prefix))))]
         (assoc result :kana (get output 0)))
       result))))


(defn flush-prefix
  "Force emit any pending output for the current prefix. Returns [kana new-prefix]
  or [nil prefix] if nothing can be emitted."
  [tree prefix]
  (if-let [output (get-in tree (concat (seq prefix) [:output]))]
    [(get output 1) ""]
    [nil prefix]))


(defn katakana-flush-prefix
  "Force emit katakana for the current prefix."
  [tree prefix]
  (if-let [output (get-in tree (concat (seq prefix) [:output]))]
    [(get output 0) ""]
    [nil prefix]))
