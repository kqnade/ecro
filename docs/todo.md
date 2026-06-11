# Refactor TODO

## Completed

- [x] Review `src/ecro/buffer.clj` responsibilities and identify one extraction target
- [x] Extract undo/redo operation logic from `buffer.clj` into `ecro.undo`
- [x] Extract text insertion/deletion helpers from `buffer.clj` (no further extraction needed)

## Pending

### High Priority

- [ ] Review `src/ecro/native.clj` FFI/event responsibilities and identify one extraction target
- [ ] Extract native event pointer decoding from `native.clj`
- [ ] Extract window tree traversal helpers from `window.clj`
- [ ] Run full Clojure tests and cljstyle after each refactor unit

### Medium Priority

- [ ] Review command dispatch shape in `src/ecro/command.clj` after main split
- [ ] Extract save-buffer command handling from `command.clj` if command dispatch grows
- [ ] Review test namespaces for `main_test` overreach after implementation namespaces exist
- [ ] Move state tests from `main_test.clj` to `state_test.clj`
- [ ] Move key handling tests from `main_test.clj` to `key_test.clj`
- [ ] Move rendering/status-line tests from `main_test.clj` to `render_test.clj`
