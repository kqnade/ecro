# ecro 開発プラン

## プロジェクト概要

ecroは、Clojureでエディタコアを記述し、RustでターミナルI/O・レンダリング・イベントループを担当する新しいTUIエディタです。GraalVM native-imageで事前コンパイルし、ネイティブバイナリとして実行します。基本操作はEmacs互換（バッファー・ウィンドウ・フレーム・ミニバッファー・M-xコマンド）を提供します。

## 技術スタック

| レイヤー | 技術 | 用途 |
|---------|------|------|
| **Rust Core** | Rust + crossterm | terminal adapter: raw mode, event loop, rendering |
| **Clojure Core** | Clojure + GraalVM native-image | バッファー、ウィンドウ、キーバインド、コマンド、モード、設定 |
| **Syntax** | tree-sitter (Rust adapter) | シンタックス解析、ハイライト |
| **FFI Bridge** | JNA (Java Native Access) | Clojure → Rust 間の通信（ポーリング型イベントキュー） |
| **設定** | EDN + SCI (Small Clojure Interpreter) | 設定ファイル |
| **テスト** | cargo test (Rust) / clojure.test (Clojure) | TDD |
| **ビルド** | Cargo + deps.edn + build.clj | 統合ビルドパイプライン |

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────┐
│                    Clojure Layer                        │
│  (GraalVM native-image)                                 │
│  - buffer / cursor / window / frame                     │
│  - keymap / command / mode                              │
│  - minibuffer / M-x                                     │
│  - file I/O                                             │
│  - config (EDN + SCI)                                   │
│  - エディタ状態機械 + main loop                         │
└──────────────────────┬──────────────────────────────────┘
                       │ JNA FFI (polling)
                       ▼
┌─────────────────────────────────────────────────────────┐
│                    Rust Layer (cdylib)                  │
│  ┌─────────────────┐  ┌─────────────────────────────┐   │
│  │  Event Loop     │  │  Rendering Engine           │   │
│  │  - key          │  │  - screen buffer            │   │
│  │  - mouse        │  │  - diff render              │   │
│  │  - resize       │  │  - crossterm output         │   │
│  └─────────────────┘  └─────────────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Terminal Adapter                               │   │
│  │  - raw mode / alternate screen                  │   │
│  │  - terminal size                                │   │
│  │  - shutdown / cleanup                           │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Syntax Adapter (tree-sitter, post-MVP)        │   │
│  │  - incremental parse                            │   │
│  │  - highlight spans                              │   │
│  │  - syntax nodes / errors                        │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  C API Bridge (ecro_core.h)                     │   │
│  │  - ecro_init() / ecro_shutdown()                 │   │
│  │  - ecro_poll_event()                             │   │
│  │  - ecro_render_frame()                           │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## 設計上の決定事項

### Rust ↔ Clojure 通信方式
- **方式**: ポーリング型（イベントキュー）
- **理由**: コールバック（C→Java方向）はGraalVM native-imageでスレッド管理が複雑になるため
- **フロー**: Rustがイベントをキューに蓄積 → Clojureが `ecro_poll_event()` で取得

### Rustの責務
- **terminal adapter に限定**: raw mode、alternate screen、terminal size、key/mouse/resize event取得、screen diff render、shutdown/cleanup
- **エディタ状態は持たない**: buffer、cursor、window、frame、keymap、mode はすべてClojure側

### レンダリング
- **方式**: Rust側でcrossterm low-levelで完全管理
- **理由**: ratatuiはC APIラップが複雑。エディタ固有のレンダリング（行番号、シンタックスハイライトなど）には自前の差分レンダリングが最適

### 設定システム
- **Phase 1**: EDNファイル（`~/.ecro/init.edn`）— keymap、theme、options
- **Phase 2**: SCI上で`~/.ecro/init.clj` を評価
- **理由**: フルClojure `require/eval` はGraalVM native-imageの閉じた世界モデルと衝突する。SCIはサンドボックス可能

設定例（EDN）:
```clojure
{:theme :dark
 :keymap {"C-x C-s" :save-buffer
          "C-x C-f" :find-file}}
```

設定例（SCI）:
```clojure
(ecro/theme :dark)
(ecro/map "C-x C-s" :save-buffer)
(ecro/command :hello (fn [] (ecro/message "hello")))
```

### tree-sitter
- **採用する**がMVP後
- **Rust側にsyntax adapter**として分離
- Clojure側は mode管理、faceマッピング、コマンド連携

### Undo/Redo
- **Phase 2 に最小版を含める**
- 最初からundo-treeは不要。stack + operation logで十分

### シンタックスハイライト
- **MVPから外す**。データモデルだけ将来対応できるよう余白を残す（face/style spanの概念）

## ディレクトリ構造

```
ecro/
├── Cargo.toml                    # Rust workspace
├── deps.edn                      # Clojure dependencies
├── build.clj                     # AOT + native-image build script
├── mise.toml                     # mise toolchain
│
├── rust/
│   └── ecro-core/
│       ├── Cargo.toml
│       ├── cbindgen.toml         # Cヘッダー自動生成設定
│       └── src/
│           ├── lib.rs            # C API定義 + module tree
│           ├── terminal.rs       # Raw mode, alternate screen, cursor
│           ├── event.rs          # Keyboard, mouse, resize events
│           ├── render.rs         # Rendering engine + screen buffer
│           └── ffi.rs            # FFI helpers (#[repr(C)] structs)
│       └── tests/
│           └── ...               # Rust unit tests
│
├── src/
│   └── ecro/
│       ├── main.clj              # Entry point (GraalVM main)
│       ├── native.clj            # JNA FFI wrapper (Rust cdylib)
│       ├── core.clj              # Editor state machine + main loop
│       ├── buffer.clj            # Buffer management (text + undo)
│       ├── window.clj            # Window / frame / split management
│       ├── keymap.clj            # Keymap + Emacs key bindings
│       ├── mode.clj              # Major / minor modes
│       ├── minibuffer.clj        # Minibuffer + prompt + M-x
│       ├── command.clj           # Command registry + execution
│       ├── file.clj              # File I/O (find-file, save-buffer)
│       └── config.clj            # Configuration system (EDN + SCI)
│
├── test/
│   └── ecro/
│       └── ...                   # Clojure unit tests
│
├── resources/
│   └── META-INF/
│       └── native-image/
│           ├── reflect-config.json    # GraalVM reflection metadata
│           ├── resource-config.json    # GraalVM resource metadata
│           └── jni-config.json         # GraalVM JNI metadata
│
├── script/
│   ├── build-rust.sh             # Rust cdylib build + cbindgen
│   └── build-native.sh           # GraalVM native-image build
│
└── docs/
    └── plan.md                   # This file
```

## 実装フェーズ

### Phase 0: プロジェクト構造セットアップ
**目標**: 空のプロジェクト構造を作成し、ビルドパイプラインを確立する

| テスト | コミット |
|-------|---------|
| Rust `ecro-core` ライブラリが `cdylib` としてビルドできる | feat: setup Rust cdylib |
| Clojure プロジェクトが `deps.edn` で起動できる | feat: setup Clojure project |
| JNA 依存が解決できる | feat: add JNA dependency |
| cbindgen で C ヘッダーが生成できる | feat: setup cbindgen |
| test runner が動作する (cargo test + clojure.test) | feat: setup test runners |

### Phase 1: Clojure buffer/cursor core
**目標**: バッファーとカーソルの基本モデルを純粋関数で構築

| テスト | コミット |
|-------|---------|
| バッファーが作成できる（名前付き） | feat: create buffer model |
| バッファーに文字を挿入できる | feat: buffer insert char |
| バッファーから文字を削除できる | feat: buffer delete char |
| カーソル位置（point）を管理できる | feat: cursor point model |
| 行・列座標に変換できる | feat: line/column coordinates |
| 行単位の編集操作 (split/join) | feat: line operations |

### Phase 2: Undo/Redo最小実装
**目標**: 操作ログベースのundo/redoを追加

| テスト | コミット |
|-------|---------|
| insert操作がundoできる | feat: undo insert |
| delete操作がundoできる | feat: undo delete |
| undoをredoできる | feat: redo operation |
| 操作ログが記録される | feat: operation log |

### Phase 3: keymap + Emacs基本コマンド
**目標**: 最小限のEmacsキーバインドを実装

| テスト | コミット |
|-------|---------|
| キーマップが定義できる (key sequence → command) | feat: keymap definition |
| C-f でカーソルを1文字進める | feat: C-f forward-char |
| C-b でカーソルを1文字戻す | feat: C-b backward-char |
| C-n で次の行へ移動 | feat: C-n next-line |
| C-p で前の行へ移動 | feat: C-p previous-line |
| C-a で行頭へ移動 | feat: C-a move-beginning-of-line |
| C-e で行末へ移動 | feat: C-e move-end-of-line |
| C-k で行末までキル（カット） | feat: C-k kill-line |
| キーマップが階層的に検索できる (prefix key) | feat: keymap prefix lookup |

### Phase 4: window/frame model
**目標**: ウィンドウとフレームの分割管理

| テスト | コミット |
|-------|---------|
| ウィンドウが作成できる（バッファー割り当て） | feat: window model |
| フレームが作成できる（ウィンドウツリー） | feat: frame model |
| ウィンドウを垂直分割できる | feat: vertical split |
| ウィンドウを水平分割できる | feat: horizontal split |
| 分割ウィンドウ間を移動できる | feat: window navigation |

### Phase 5: file I/O
**目標**: ファイルを開いて保存できる

| テスト | コミット |
|-------|---------|
| ファイルを読み込んでバッファーに格納できる | feat: file read |
| バッファー内容をファイルに書き込める | feat: file write |
| C-x C-f (find-file) コマンド | feat: find-file command |
| C-x C-s (save-buffer) コマンド | feat: save-buffer command |

### Phase 6: Rust terminal adapter
**目標**: crosstermでraw mode、alternate screen、イベント取得を実装

| テスト | コミット |
|-------|---------|
| Raw mode に切り替えられる | feat: raw mode toggle |
| Alternate screen に切り替えられる | feat: alternate screen |
| キーイベント（KeyEvent）を読み取れる | feat: key event polling |
| マウスイベントを読み取れる | feat: mouse event |
| リサイズイベントを取得できる | feat: resize event |
| 画面サイズ（width/height）を取得できる | feat: terminal size |
| 画面にテキストを出力できる | feat: screen render |

### Phase 7: JNA FFI integration
**目標**: RustのC APIをJNA経由でClojureから呼び出す

| テスト | コミット |
|-------|---------|
| ecro_init() / ecro_shutdown() をJNAで呼び出せる | feat: init/shutdown FFI |
| ecro_poll_event() でイベントをClojure側に受け渡せる | feat: event FFI bridge |
| #[repr(C)] 構造体がJNAで正しくマッピングされる | feat: C struct mapping |
| 文字列（CString → String）を受け渡せる | feat: string FFI |
| レンダリング命令をRustに送れる | feat: render FFI |

### Phase 8: real TUI main loop
**目標**: Clojure main loop + Rust terminal adapterを統合

| テスト | コミット |
|-------|---------|
| エディタの開始/終了が正常に行える | feat: editor lifecycle |
| キー入力→コマンド実行→画面更新のサイクル | feat: input-command-render cycle |
| Ctrl-C等の例外時にterminalが復元される | feat: graceful shutdown |

### Phase 9: minibuffer + M-x
**目標**: ミニバッファー、プロンプト、M-xコマンド実行

| テスト | コミット |
|-------|---------|
| ミニバッファー（特殊バッファー）が作成できる | feat: minibuffer model |
| ミニバッファーに入力できる | feat: minibuffer input |
| コマンドをレジストリに登録できる | feat: command registry |
| M-x でコマンドを実行できる | feat: M-x execute-command |
| ミニバッファーでファイル名を補完できる | feat: minibuffer completion |

### Phase 10: EDN config
**目標**: EDNベースの設定システム

| テスト | コミット |
|-------|---------|
| ~/.ecro/init.edn を読み込める | feat: load EDN config |
| キーバインドを設定ファイルで上書きできる | feat: config keymap override |
| テーマ（前景/背景色）を設定できる | feat: theme config |

### Phase 11: SCI config
**目標**: SCIによるClojure風設定

| テスト | コミット |
|-------|---------|
| ~/.ecro/init.clj をSCIで評価できる | feat: SCI config loader |
| SCIからecro APIを呼び出せる | feat: SCI API bridge |

### Phase 12: mode system
**目標**: メジャーモード・マイナーモード管理

| テスト | コミット |
|-------|---------|
| メジャーモードをバッファーに設定できる | feat: major mode |
| マイナーモードをトグルできる | feat: minor mode |
| モードによってキーマップが切り替わる | feat: mode-specific keymap |
| fundamental-mode / text-mode 基本実装 | feat: basic modes |

### Phase 13: tree-sitter syntax adapter
**目標**: Rust側のtree-sitter adapter

| テスト | コミット |
|-------|---------|
| tree-sitter languageをロードできる | feat: load tree-sitter language |
| テキストをインクリメンタルパースできる | feat: incremental parse |
| パース結果（syntax tree）を取得できる | feat: syntax tree query |
| ハイライト用のspanを生成できる | feat: highlight spans |

### Phase 14: syntax highlight
**目標**: tree-sitter + faceモデルでシンタックスハイライトを実装

| テスト | コミット |
|-------|---------|
| face（前景・背景・太字・斜体）を定義できる | feat: face model |
| tree-sitter結果からface spanを生成できる | feat: face span mapping |
| 画面にハイライトテキストを表示できる | feat: syntax highlight render |
| Clojure / Rust / Markdownのハイライト | feat: language highlights |

### Phase 15: indentation / structural editing
**目標**: インデントと構造的編集

| テスト | コミット |
|-------|---------|
| インデントルールを定義できる | feat: indent rules |
| TABでインデントを調整できる | feat: TAB indent |
| 括弧・ブロックの対応付け | feat: bracket matching |
| 自動インデント（改行時） | feat: auto-indent on newline |

## TDD & コミット戦略

### テストフレームワーク
- **Rust**: `cargo test`
- **Clojure**: `clojure.test`

### コミット戦略
Redコミット（失敗テストのみ）を履歴に残さず、**テスト追加 + 最小実装を1コミット**にまとめる。

```text
feat: add buffer creation test and implementation
feat: add cursor forward-char behavior
refactor: simplify buffer cursor update
fix: handle buffer edge case for empty lines
```

追跡性はコミットメッセージでテスト対象を明記することで担保する。

### コミットメッセージ規約

| プレフィックス | 用途 |
|---------------|------|
| `feat:` | 新機能実装（テスト含む） |
| `fix:` | バグ修正 |
| `refactor:` | リファクタリング（動作不変） |
| `test:` | テスト追加（実装変更なし） |
| `chore:` | ビルド・CI・プロジェクト設定 |
