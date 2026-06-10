# ecro ビルド成果物

## ビルド済み成果物

### Rust cdylib
```
rust/ecro-core/target/release/libecro_core.so
```

### 実行方法

```bash
# 方法1: ラッパースクリプトを使用
./script/run.sh

# 方法2: 直接実行
export LD_LIBRARY_PATH=$(pwd)/rust/ecro-core/target/release:$LD_LIBRARY_PATH
clojure -M -m ecro.main
```

## キーバインド

- `C-f` - カーソル進む
- `C-b` - カーソル戻る
- `C-n` - 次の行
- `C-p` - 前の行
- `C-a` - 行頭
- `C-e` - 行末
- `C-k` - 行末まで削除
- `C-x C-f` - ファイルを開く
- `C-x C-s` - ファイルを保存

## 終了方法

現在は `Ctrl-C` で強制終了してください（graceful shutdown実装中）。

## テスト実行

```bash
# 全テスト実行
export LD_LIBRARY_PATH=$(pwd)/rust/ecro-core/target/release:$LD_LIBRARY_PATH
clojure -M:test

# Rustテスト
cd rust/ecro-core && cargo test
```
