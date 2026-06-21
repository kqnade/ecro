# TODO

## Next

### Mode detection

- [x] Detect major mode from file extension / buffer name
- [ ] Add basic `fundamental-mode`
- [ ] Add basic `text-mode`
- [x] Add major mode support on buffers
- [ ] Add minor mode toggle support
- [ ] Add mode-specific keymap resolution

### mise project tooling detection

- [ ] Walk parent directories from buffer path up to HOME or git root to find `mise.toml`
- [ ] Parse `mise.toml` `[tools]` for project tooling detection
- [ ] Normalize mise tool names into a keyword set
- [ ] Cache mise detection result globally
- [ ] Infer analyzer / LSP candidates from detected mise tools
- [ ] Return an empty mise detection result when `mise.toml` is missing
- [ ] Keep mise tooling separate from ecro editor config

## Medium

- [ ] Implement `M-f` `forward-word`
- [ ] Implement `M-b` `backward-word`
- [ ] Implement `M-,` `beginning-of-buffer`
- [ ] Implement `M-.` `end-of-buffer`
- [ ] Implement `C-v` `scroll-up-command`
- [ ] Implement `M-v` `scroll-down-command`
- [ ] Implement `M-y` `yank-pop`
- [ ] Implement `ESC b` `switch-to-buffer`
- [ ] Implement `ESC k` `kill-buffer`
- [ ] Implement `ESC B` `list-buffers`
- [ ] Implement `ESC 0` `delete-window`
- [ ] Implement `ESC 1` `delete-other-windows`
- [ ] Implement `ESC o` `other-window`
- [ ] Implement `ESC w` `write-file`
- [ ] Implement `C-SPC` `set-mark-command`
- [ ] Implement region highlight / region-based commands

## Low

- [ ] Implement `ESC %` `query-replace`
- [ ] Implement `C-l` `recenter-top-bottom`
- [ ] Add face model for syntax highlight
- [ ] Integrate tree-sitter syntax adapter in Rust
- [ ] Implement indentation rules and `TAB` indent
- [ ] Implement bracket matching
- [ ] Implement auto-indent on newline
- [ ] Integrate SKK Japanese input mode

## Completed

- [x] Detect major mode from file extension / buffer name
- [x] Add major mode support on buffers
- [x] Review `src/ecro/buffer.clj` responsibilities and identify one extraction target
- [x] Extract undo/redo operation logic from `buffer.clj` into `ecro.undo`
- [x] Extract text insertion/deletion helpers from `buffer.clj` (no further extraction needed)
- [x] Review `src/ecro/native.clj` FFI/event responsibilities and identify one extraction target
- [x] Extract native event pointer decoding from `native.clj`
- [x] Extract window tree traversal helpers from `window.clj`
- [x] Run full Clojure tests and cljstyle after each refactor unit
- [x] Review command dispatch shape in `src/ecro/command.clj` after main split
- [x] Extract save-buffer command handling from `command.clj` (already extracted)
- [x] Review test namespaces for `main_test` overreach after implementation namespaces exist
- [x] Move state tests from `main_test.clj` to `state_test.clj`
- [x] Move key handling tests from `main_test.clj` to `key_test.clj`
- [x] Move rendering/status-line tests from `main_test.clj` to `render_test.clj`
- [x] Fix nested `deftest` forms in `test/ecro/state_test.clj`
- [x] Make `C-k` kill-line record an undoable delete operation
- [x] Make `native/get-terminal-size` return `nil` when Rust terminal size lookup fails
- [x] Add valid `:build` alias and `build.clj` for native-image AOT
- [x] Initialize and preserve `:saved-text` so modified markers are accurate
- [x] Replace hardcoded `/tmp/ecro_test.txt` find-file command behavior with a new empty buffer
- [x] Clamp rendered status line to terminal width
- [x] Add real `M-x` command execution coverage with `mx-execute`
- [x] Document root-only split behavior with a non-root split test
- [x] Implement real `find-file` minibuffer flow: prompt for a path, read minibuffer input, and open that file
- [x] Add SCI config loader for `~/.ecro/init.clj`
- [x] Expose minimal SCI `ecro/*` API for config commands such as keymap, theme, and command registration
