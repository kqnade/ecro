# ecro — AI Agent Instructions

## Project

ecro: Emacs-inspired TUI editor. Core logic in Clojure (GraalVM native-image). Terminal adapter in Rust (crossterm, cdylib). FFI via JNA polling event queue.

## Build & Test

```bash
# Rust — build cdylib
cargo build --release
cargo test

# Rust — lint
cargo clippy -- -D warnings
cargo fmt --check

# Clojure — run tests
clojure -M:test

# Clojure — lint
cljstyle check

# Full native build (GraalVM)
script/build-rust.sh
script/build-native.sh
```

## TDD Workflow

1. Write test first, commit as `feat: add test and impl for X`
2. Implement minimum to pass
3. Refactor if needed, commit as `refactor: …`
4. Run lint before declaring done

## Commit Granularity

- Commit by implementation unit, not by broad feature bundle.
- Keep each TDD cycle as small as possible: one behavior, one focused test, minimum implementation, CI/lint, then commit.
- Do not combine independent implementations in one commit. For example, cut/copy/paste and Shift+Arrow selection are separate implementation units.
- Do not start the next implementation unit until the current one has passing tests and has been committed.
- If a change naturally splits into core model, command integration, and docs, commit each coherent unit separately after its relevant tests/checks pass.

Test frameworks:
- Rust: `cargo test`
- Clojure: `clojure.test`

## Commit Conventions

Use [Gitmoji](https://gitmoji.dev/) for all commits.

| Gitmoji | Prefix | Use |
|---------|--------|-----|
| ✨ | `feat:` | new feature (test + impl together) |
| 🐛 | `fix:` | bug fix |
| ♻️ | `refactor:` | behavior-preserving change |
| ✅ | `test:` | test-only addition |
| 🔧 | `chore:` | build, config, project setup |
| 📝 | `docs:` | documentation changes |
| 🎨 | `style:` | formatting, no logic change |
| ⚡️ | `perf:` | performance improvement |
| 🔥 | `remove:` | remove code/files |

**Do NOT sign commits.** Use `git commit` without GPG signing.

## Architecture

```
Clojure (editor core) ──JNA polling──► Rust (terminal adapter)
  - buffer/cursor/window/frame            - raw mode
  - keymap/command/mode                   - event loop (key, mouse, resize)
  - minibuffer/M-x                        - screen diff render
  - file I/O                              - tree-sitter syntax (post-MVP)
  - EDN + SCI config
```

- Rust must NOT hold editor state — it is a stateless terminal adapter.
- Clojure holds all editor meaning.
- FFI boundary is a polling event queue, not callbacks.

## Directory Map

```
src/ecro/          Clojure namespaces
test/ecro/         Clojure tests
rust/ecro-core/    Rust cdylib + C API + tests
resources/         GraalVM native-image metadata
script/            build scripts
docs/              documentation
```

## Key Dependencies

- `org.clojure/clojure` — Clojure runtime
- `net.java.dev.jna/jna` — JNA FFI
- `borkdude/sci` — Small Clojure Interpreter (config)
- `crossterm` (Rust) — terminal I/O
- `tree-sitter` (Rust, post-MVP) — syntax parsing

## Config

- `~/.ecro/init.edn` — EDN config (Phase 10)
- `~/.ecro/init.clj` — SCI-evaluated config (Phase 11)
