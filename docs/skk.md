# ecro SKK Implementation Plan

## Goal

Implement a small, DDSKK-inspired SKK input method for ecro.

The first target is not full DDSKK compatibility. The target is a usable SKK core:

- `ESC n` toggles SKK minor mode for the current buffer.
- Kana mode converts romaji to hiragana/katakana.
- Uppercase input starts henkan point, DDSKK-style.
- `SPC` searches SKK dictionaries and cycles candidates.
- `C-g` cancels conversion.
- `RET` and `C-m` confirm active conversion.
- Personal dictionary is read from the user's SKK files, including the dictionary configured from `~/.skk` when possible.

## DDSKK Shape To Copy

DDSKK's implementation splits the behavior into these concerns:

- `skk-mode`: top-level minor mode.
- Submodes: `skk-j-mode` (kana), `skk-latin-mode` (ASCII), `skk-jisx0208-latin-mode` (wide ASCII), `skk-abbrev-mode`.
- `skk-insert`: main dispatcher for printable input.
- `skk-kana-input`: romaji-to-kana state machine.
- `skk-rom-kana-base-rule-list` and `skk-rom-kana-rule-list`: input rules in `(input next-state output)` form.
- `skk-rule-tree`: compiled trie of romaji rules.
- `skk-henkan-mode`: conversion state, effectively nil / on / active.
- `skk-henkan-start-point`, `skk-henkan-end-point`: buffer-local conversion range markers.
- `skk-okurigana`, `skk-okuri-char`: okurigana state.
- `skk-search-prog-list`: ordered dictionary search pipeline.
- `skk-jisyo`: personal dictionary, defaulting to `~/.skk-jisyo` unless configured.

ecro should copy the separation, not the Emacs-specific mechanisms. In particular, do not copy overlays, marker objects, post-command hooks, modeline integration, or Elisp customization machinery directly.

## Dictionary Format

SKK dictionaries are plain text. The important structure is:

```text
;; okuri-ari entries.
うごk /動/動か/
かk /書/描/
;; okuri-nasi entries.
かな /仮名/かな/
にほん /日本/二本/
```

Rules:

- Lines beginning with `;;` are headers/comments.
- Okuri-ari entries come before `;; okuri-nasi entries.`.
- Okuri-nasi entries come after `;; okuri-nasi entries.`.
- A line is `midashi /candidate1/candidate2/.../`.
- Candidate annotations can appear after `;`; MVP should keep the candidate string before `;` and ignore annotations.
- Okuri-ari midashi usually ends in a roman okuri key, e.g. `うごk`.

Dictionary discovery should use this order:

1. ecro config value, later exposed as `:skk {:jisyo-path ... :large-jisyo-path ...}`.
2. If `~/.skk` exists, extract simple `(setq skk-jisyo "...")` and `(setq skk-large-jisyo "...")` forms with a conservative regexp. Do not evaluate Elisp.
3. Personal dictionary default: `~/.skk-jisyo`.
4. Optional large dictionary path if configured. No bundled large dictionary initially.

The user mentioned having SKK dictionary configuration in `~/.skk`, so step 2 is worth implementing early, but it must be best-effort and safe.

## ecro State Model

SKK state should be buffer-local because input mode and conversion range belong to a buffer.

Add `:skk` to buffers only when SKK is enabled:

```clojure
{:mode :hiragana          ; :hiragana, :katakana, :latin, :abbrev
 :henkan-mode nil         ; nil, :on, :active
 :kana-prefix ""          ; current romaji prefix waiting in the rule trie
 :henkan-start nil        ; integer buffer offset
 :henkan-end nil          ; integer buffer offset
 :henkan-key nil          ; hiragana midashi used for search
 :okuri-char nil          ; roman okuri key, e.g. "k"
 :okurigana ""            ; kana okurigana visible in buffer
 :candidates []
 :candidate-index 0}
```

Keep this inside `:current-buffer`; `state/assoc-current-buffer` will keep the buffer list synchronized.

## Proposed Namespaces

Start small:

- `ecro.skk`: state machine, romaji rules, key handling.
- `ecro.skk.jisyo`: parser and dictionary lookup once parser code is non-trivial.

Avoid splitting more until needed.

## Key Integration

ecro should use Kitty keyboard protocol before full SKK key handling. SKK should be able to distinguish `C-m` from `RET`, so the terminal adapter must pass key identity and modifiers without collapsing them into the same legacy control code. During active SKK conversion, both `RET` and `C-m` intentionally confirm the current candidate.

Current key handling goes through `ecro.key/handle-key`. Add SKK interception before normal printable insertion:

1. If minibuffer is active, do not use SKK initially.
2. If current buffer does not have `:skk-mode`, keep existing behavior.
3. If SKK is active, pass printable keys plus `SPC`, `RET`, `C-m`, `BS`, and `C-g` to `ecro.skk/handle-key` first.
4. If SKK consumes the key, return the updated editor state.
5. Otherwise fall through to normal editor commands.

This matches DDSKK's approach where SKK mode keymaps intercept printable input but can emulate the original keymap when a key should not be consumed.

## Input Behavior MVP

### Toggle

- `ESC n` calls `:toggle-skk`.
- Enabling SKK initializes `:skk` state with `:mode :hiragana`.
- Disabling SKK confirms or cancels pending composition. Prefer confirm for already inserted text, cancel for unresolved prefixes.

### Kana Input

Implement DDSKK-style romaji rule entries:

```clojure
["ka" nil ["カ" "か"]]
["kk" "k" ["ッ" "っ"]]
["nn" nil ["ン" "ん"]]
["n'" nil ["ン" "ん"]]
```

Compile rules into a trie. On each key:

- If `kana-prefix + key` can continue in the trie, update `:kana-prefix` and show the prefix in-buffer or status.
- If it reaches an output, insert hiragana or katakana depending on `:mode`, then set `:kana-prefix` to `next-state`.
- If it cannot continue, flush any valid output and reprocess the key from the root.

The first implementation may directly insert temporary prefix text into the buffer, but the preferred implementation is to keep unresolved prefix in `:skk` and render it at point. Direct insertion is simpler; render-at-point gives better undo behavior.

### Submodes

Implement these DDSKK-compatible controls:

- `q`: toggle hiragana / katakana.
- `l`: enter latin mode.
- `C-m`: return from latin mode to hiragana, or confirm conversion.
- `/`: enter abbrev mode later; skip in MVP unless dictionary search for ASCII midashi is implemented.

### Henkan Point

DDSKK starts conversion input with uppercase letters.

MVP behavior:

- Uppercase ASCII in hiragana/katakana mode starts `:henkan-mode :on`.
- Store `:henkan-start` at current point before inserting the first kana.
- Downcase the uppercase key before romaji conversion.
- Show an indicator in status line, e.g. `SKK:▽かな` while reading is being typed.

Example:

```text
Benri SPC -> 便利
```

Internal reading before `SPC` is `べんり`.

### Conversion

When `SPC` is pressed in `:henkan-mode :on`:

- Determine `:henkan-key` from buffer text between `:henkan-start` and point.
- Normalize katakana to hiragana before dictionary lookup.
- Search dictionaries in configured order.
- If candidates exist, replace the henkan range with the first candidate.
- Set `:henkan-mode :active`, `:henkan-end`, `:candidates`, `:candidate-index 0`.
- If no candidates exist, keep kana and show `No SKK candidates: <midashi>`.

When `SPC` is pressed in `:henkan-mode :active`:

- Cycle to the next candidate.
- Replace the current candidate range with the selected candidate.
- Wrap around only if we intentionally want DDSKK-like circulation; otherwise stop at the last candidate for MVP.

When `RET` or `C-m` is pressed in `:active`:

- Confirm current candidate.
- Clear henkan state.
- Later, update personal dictionary candidate order.

When `C-g` is pressed:

- If `:active`, replace candidate with original kana and clear conversion.
- If `:on`, cancel henkan point and keep kana text.
- If only `:kana-prefix` exists, clear it.

### Okurigana

DDSKK uses uppercase letters to mark both the start and end of the stem for inflected words.

Example from DDSKK docs:

```text
TuyoI -> 強い
```

MVP implementation:

- When already in `:henkan-mode :on`, a second uppercase key marks okuri start.
- The second uppercase key is downcased and processed as kana input for okurigana.
- Store `:okuri-char` as the roman character, e.g. `"i"`.
- Dictionary key is `henkan-key + okuri-char`, e.g. `つよi`.
- Candidate replaces the stem plus okurigana range.

Do this after okuri-nasi conversion works.

## Rendering

Minimum status line additions:

- SKK disabled: no indicator.
- Hiragana: `SKK:かな`.
- Katakana: `SKK:カナ`.
- Latin: `SKK:latin`.
- Henkan reading: `SKK:▽ <midashi>`.
- Active conversion: `SKK:▼ <candidate-index>/<candidate-count>`.

Inline candidate popup should be Phase 2. DDSKK has dynamic completion and candidate overlays, but ecro should first show candidates in the status line or a temporary `:notification`.

## Dictionary Search Plan

Implement parser tests first.

Parser output:

```clojure
{:okuri-ari {"うごk" ["動" "動か"]}
 :okuri-nasi {"にほん" ["日本" "二本"]}}
```

Search order:

1. Personal dictionary candidates from `~/.skk-jisyo` or configured `skk-jisyo`.
2. Large dictionary candidates from configured `skk-large-jisyo`.
3. Optional identity fallback: return the original kana.

MVP can load dictionaries lazily once into editor state or a namespace-level cache. Use mtime-based reload later.

Do not save personal dictionary in the first conversion implementation. Add saving only after confirmation behavior is stable.

## Commit-Sized Implementation Units

1. `feat: add Kitty keyboard event model`
   - Rust terminal adapter enables Kitty keyboard protocol when supported.
   - Clojure key events preserve key identity and modifiers separately from legacy control bytes.
   - Tests cover `C-m` as distinct from `RET`.

2. `feat: parse SKK jisyo files`
   - Add parser for okuri-ari / okuri-nasi sections.
   - Add tests for comments, annotations, duplicate entries, and candidate order.

3. `feat: load SKK dictionary paths`
   - Add safe path discovery: ecro config, conservative `~/.skk` extraction, default `~/.skk-jisyo`.
   - Add tests with temp files.

4. `feat: add SKK romaji rule tree`
   - Port DDSKK's `(input next-state output)` shape.
   - Implement trie compile and step function.
   - Test `ka`, `kya`, `nn`, `n'`, doubled consonants, and katakana output.

5. `feat: handle SKK kana input`
   - Initialize `:skk` on `ESC n` enable.
   - Intercept printable keys when `:skk-mode` is active.
   - Insert kana into the buffer.
   - Status line shows SKK mode.

6. `feat: start SKK conversion with uppercase input`
   - Uppercase key sets henkan point and downcases input.
   - `SPC` starts dictionary lookup for okuri-nasi words.

7. `feat: cycle and confirm SKK candidates`
   - `SPC` cycles candidates in active conversion.
   - `RET` and `C-m` confirm.
   - `C-g` cancels.

8. `feat: support SKK okurigana conversion`
   - Second uppercase key captures okuri char.
   - Search okuri-ari section.
   - Confirm/cancel replaces the correct region.

9. `feat: update personal SKK dictionary`
   - On confirm, move selected candidate to front for the midashi.
   - Add missing candidate entries when word registration exists.
   - Save atomically with backup.

10. `feat: show SKK candidates`
   - Add candidate list display in status/notification first.
   - Later add popup rendering near point.

## Non-Goals For First Version

- Full Elisp `~/.skk` evaluation.
- DDSKK dynamic completion (`skk-dcomp`) compatibility.
- SKK server protocol.
- Numeric conversion.
- Annotation display/editing.
- Word registration minibuffer.
- AZIK/ACT/TUT-code.
- JISX0208 latin mode beyond a simple placeholder.

## First Test Cases

Use Clojure tests around pure functions before wiring into `ecro.key`.

- Parse dictionary line: `にほん /日本/二本/`.
- Parse annotated candidate: `かな /仮名;annotation/` returns `仮名`.
- Parse okuri-ari line before `;; okuri-nasi entries.`.
- Discover `skk-jisyo` path from a temp `~/.skk`-style file without evaluating Elisp.
- Romaji step `k` waits, `ka` emits `か`.
- Romaji step `kk` emits `っ` with next prefix `k`.
- Hiragana/katakana toggle changes output side.
- Uppercase `B` starts henkan and inserts `b` as kana input.
- `Benri SPC` replaces `べんり` with first candidate.
- `SPC` during active conversion cycles to the next candidate.
- `C-g` during active conversion restores `べんり`.
