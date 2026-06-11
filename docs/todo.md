# Refactor TODO

## Completed

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
