# ecro キーバインド設計

## 基本方針

- Emacs標準キーバインドをベースとする
- **LeadKey**: `ESC`（設定可能: `lead-key`）
- 物理キーボードレイアウト（`docs/layout.md`）に合わせた最適化も行う
- レイヤー2（編集/ナビゲーション）の直接キーアクセスを優先的に活用

---

## LeadKey（設定可能）

```clojure
;; デフォルト
(reset! ecro.main/lead-key "ESC")

;; 変更例
(reset! ecro.main/lead-key "SPC")
(reset! ecro.main/lead-key "C-g")
```

LeadKey + キー でコマンド実行：
- `ESC f` → find-file
- `ESC s` → save-buffer
- `ESC u` → undo
- `ESC ESC` → keyboard-quit（プレフィックスキャンセル）

---

## 物理キーレイアウト参照

ユーザーの分割キーボード（Cokahleth類似配列）。ホームポジションは `A R S T N E I O`。

### レイヤー0（ベース）

```
左中:  LT3(TAB)  KC_A      KC_R      KC_S      KC_T      KC_G      KC_LANG2
左親:  -         -         -         KC_ESCAPE LT2(TAB)  LSFT_T(SPACE) -
```

- **LT3(TAB)**: 左手小指左横（旧 KC_TAB）。長押しで Layer 3 へ
- **LT2(TAB)**: 親指。長押しで Layer 2（編集/ナビゲーション）へ

### レイヤー2（編集/ナビゲーション）- LT2(TAB) 長押しで発動

```
左上:  ESC       F1        F2        F3        F4        F5        F5
左中:  SPACE     C-a       C-z       END       HOME      DELETE    LANG2
左下:  LCTRL     C-S-z     C-x       C-c       C-v       INSERT    -
左親:  -         -         -         TRNS      TRNS      SPACE     -

右上:  F7        F8        F9        F10       F11       F12       BSPC
右中:  LANG1     ←         ↓         ↑         →        PGUP      PAUSE
右下:  -         MEDIA_PREV MEDIA_STOP MEDIA_PLAY MEDIA_NEXT PGDN    RCTRL
右親:  -         ENTER     LALT(SPACE) LGUI    -         -         -
```

### レイヤー3（Meta修飾）- LT3(TAB) 長押しで発動

**Layer 0 + Meta 修飾キー用。**

旧 KC_TAB の位置（左手小指左横）を LT3(TAB) に変更したため、Layer 3 が利用可能に。

**設計方針**:
- Layer 3 はリセット済み・利用可能
- Layer 0 の主要文字キー位置に Meta 修飾を付与
- Layer 2 と同じ位置の矢印キーは Meta + Arrow として使う想定
- 小指外側、親指、言語切替、Control などは `KC_NO` または透過
- Alt 同時押し不要。LT3(TAB) ホールド + 通常キー位置で Meta コマンド実行

具体配列は未確定。確定済みの物理配置は `docs/layout.md` を参照。

**主要 M-キー候補（コマンド側の割り当て候補）**:
- `M-f` → `forward-word`
- `M-b` → `backward-word`
- `M-d` → `kill-word`
- `M-w` → `kill-ring-save` (copy)
- `M-y` → `yank-pop`
- `M-v` → `scroll-down-command`
- `M-,` / `M-.` → buffer先頭/末尾などの移動候補

---

## キーバインド一覧

### 移動系

| キー | コマンド | 実装 |
|------|---------|------|
| `←` | `backward-char` | ✅ |
| `↓` | `next-line` | ✅ |
| `↑` | `previous-line` | ✅ |
| `→` | `forward-char` | ✅ |
| `HOME` | `move-beginning-of-line` | ✅ |
| `END` | `move-end-of-line` | ✅ |
| `C-a` | `move-beginning-of-line` | ✅ |
| `C-e` | `move-end-of-line` | ✅ |
| `M-f` | `forward-word` | ❌ |
| `M-b` | `backward-word` | ❌ |
| `M-,` | `beginning-of-buffer` 候補 | ❌ |
| `M-.` | `end-of-buffer` 候補 | ❌ |

### スクロール系

| キー | コマンド | 実装 |
|------|---------|------|
| `PGDN` | 画面ダウンスクロール | ✅ |
| `PGUP` | 画面アップスクロール | ✅ |
| `C-v` | `scroll-up-command` (1画面下へ) | ❌ |
| `M-v` | `scroll-down-command` (1画面上へ) | ❌ |

### 編集系

| キー | コマンド | 実装 |
|------|---------|------|
| `BS` | `delete-backward-char` | ✅ |
| `DEL` | `delete-char-forward` | ✅ |
| `RET` | `newline` | ✅ |
| `TAB` | `insert-tab` (space/tab configurable) | ✅ |
| `C-k` | `kill-line` | ✅ |
| `C-d` | `delete-char-forward` | ❌ |
| `C-x` | `kill-region` (カット) | ❌ |
| `C-c` | `kill-ring-save` (コピー) | ❌ |
| `C-v` | `yank` (ペースト) | ❌ |
| `M-y` | `yank-pop` (cycle kill-ring) | ❌ |

### Undo/Redo

| キー | コマンド | 実装 |
|------|---------|------|
| `C-/` | `undo` | ✅ |
| `C-z` | `undo` (レイヤー2: C-z あり) | ✅ |
| `C-S-z` | `redo` (レイヤー2: C-S-z あり) | ✅ |
| `ESC u` | `undo` | ✅ |

> 注: レイヤー2で `C-z` と `C-S-z` が直接アクセス可能なため、undo/redoに割り当て。
> Emacsの伝統的な `C-/` もサポートする。

### ファイル操作

| キー | コマンド | 実装 |
|------|---------|------|
| `ESC f` | `find-file` | ✅ |
| `ESC s` | `save-buffer` | ✅ |
| `ESC w` | `write-file` (別名で保存) | ❌ |

### バッファー操作

| キー | コマンド | 実装 |
|------|---------|------|
| `ESC b` | `switch-to-buffer` | ❌ |
| `ESC k` | `kill-buffer` | ❌ |
| `ESC B` | `list-buffers` | ❌ |

### ウィンドウ操作

| キー | コマンド | 実装 |
|------|---------|------|
| `ESC 0` | `delete-window` | ❌ |
| `ESC 1` | `delete-other-windows` | ❌ |
| `ESC 2` | `split-window-below` | ✅（基礎） |
| `ESC 3` | `split-window-right` | ✅（基礎） |
| `ESC o` | `other-window` | ❌ |

### 検索 / 置換

| キー | コマンド | 実装 |
|------|---------|------|
| `C-s` | `isearch-forward` | ✅（search lib） |
| `C-r` | `isearch-backward` | ✅（search lib） |
| `ESC %` | `query-replace` | ❌ |

### リージョン操作

| キー | コマンド | 実装 |
|------|---------|------|
| `C-SPC` | `set-mark-command` | ❌ |
| `ESC h` | `mark-whole-buffer` | ❌ |

### モード

| キー | コマンド | 実装 |
|------|---------|------|
| `M-x` | `execute-extended-command` | ✅（基礎） |

### その他

| キー | コマンド | 実装 |
|------|---------|------|
| `C-g` / `ESC` | `keyboard-quit` | ✅ |
| `C-c C-c` | エディタ終了 | ❌ |
| `C-l` | `recenter-top-bottom` | ❌ |
| `F1`-`F24` | (将来的に割当) | ✅（認識のみ） |

---

## レイヤー2最適化マッピング

ユーザーのレイヤー2物理キーからダイレクトアクセスできるEmacs機能：

```
C-a   → move-beginning-of-line   (行頭)
C-z   → undo                     (UNDO)
C-S-z → redo                     (REDO)  
C-x   → kill-region              (カット)
C-c   → kill-ring-save           (コピー)
C-v   → yank                     (ペースト)
HOME  → move-beginning-of-line   (行頭)
END   → move-end-of-line         (行末)
DEL   → delete-char-forward      (前方削除)
←↓↑→  → カーソル移動             (方向キー)
PGUP  → 画面アップスクロール
PGDN  → 画面ダウンスクロール
```

> 注: 標準のEmacsキー（C-f, C-b, C-n, C-p等）も併用可能。

---

## 実装優先順位

1. **Phase A（必須）** — 完成度に直結
   - ✅ kill-ring: `C-k` `kill-line`
   - undo/redo: `C-z` `C-S-z` `C-/` `ESC u`
   - kill-region/copy/yank: `C-x` `C-c` `C-v`
   - 複数バッファー: `ESC b` `ESC k`

2. **Phase B（重要）** — 生産性向上
   - ウィンドウ操作: `ESC 0` `ESC 1` `ESC o`
   - 置換: `ESC %`
   - マクロ: `ESC (` `ESC )` `ESC e`
   - リージョン: `C-SPC`

3. **Phase C（拡張）** — 使い勝手
   - word移動: `M-f` `M-b`
   - buffer端移動: `M-,` `M-.`
   - file tree jumper: ポップアップ `ESC d`
   - `C-l` recenter

---

## 設計上の注意

- **LeadKey**: `ESC` をデフォルトとし、設定可能にする
  - `C-a` や `SPC` への変更をサポート
  - keymapは動的に再構築される
- `C-x` はEmacsではプレフィックスだが、レイアウト上直接アクセス可のため `kill-region`（カット）に割り当てる
- `C-c` はEmacsでは「ユーザー定義プレフィックス」だが、レイアウト上直接アクセス可のため `kill-ring-save`（コピー）に割り当てる
- `C-v` はEmacsでは `scroll-up-command` だが、レイアウト上直接アクセス可のため `yank`（ペースト）に割り当てる
- undo/redoはレイアウト上の `C-z` `C-S-z` を優先し、Emacs伝統の `C-/` も併用
- ユーザーはレイヤー2の矢印キーでカーソル移動できるため、`C-f/b/n/p` は必須ではないが互換性のために維持
