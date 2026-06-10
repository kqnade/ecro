# def.json 配列構造

`def.json` は Vial/VIA 形式のキーマップ定義です。トップレベルでは、物理キー配列、ロータリーエンコーダ、マクロ、タップダンス、コンボ、キーオーバーライドなどを配列として保持しています。

## トップレベル

| キー | 型 | 内容 |
| --- | --- | --- |
| `version` | 数値 | 定義ファイルのバージョン。 |
| `uid` | 数値 | キーボード定義を識別する UID。 |
| `layout` | 配列 | キーごとの割り当て。6 レイヤー分ある。 |
| `encoder_layout` | 配列 | ロータリーエンコーダの割り当て。6 レイヤー分ある。 |
| `layout_options` | 数値 | レイアウトオプションのビット値。 |
| `macro` | 配列 | マクロ定義。16 スロットある。 |
| `vial_protocol` | 数値 | Vial プロトコルバージョン。 |
| `via_protocol` | 数値 | VIA プロトコルバージョン。 |
| `tap_dance` | 配列 | Tap Dance 定義。32 スロットある。 |
| `combo` | 配列 | Combo 定義。32 スロットある。 |
| `key_override` | 配列 | Key Override 定義。32 スロットある。 |
| `alt_repeat_key` | 配列 | Alternate Repeat Key 定義。現状は空。 |
| `settings` | オブジェクト | ファームウェア設定値。キーは文字列の ID。 |

## `layout`

`layout` は次の 3 段構造です。

```text
layout[layer][row][column] = keycode
```

- レイヤー数は 6。
- 各レイヤーは 8 行。
- 各行は 7 要素。
- `-1` は物理キーが存在しない位置。
- `KC_TRNS` は下位レイヤーを透過するキー。
- `KC_NO` は何もしないキー。
- `LTn(KEY)` は押下で `KEY`、長押しまたはホールドでレイヤー `n` を有効化するキー。
- `LSFT_T(KEY)` / `RSFT_T(KEY)` はタップで `KEY`、ホールドで Shift になる Mod-Tap キー。
- `LCTL(KEY)`、`LSFT(KEY)`、`RCTL(KEY)`、`C_S(KEY)` などは修飾キー付き入力。

### レイヤー 0

通常入力用のベースレイヤーです。左右 4 行ずつに分かれた分割キーボード配置として読めます。`def.json` の右手側は外側から内側の順に入っているため、下の物理配置図では右手側を左右反転して表示しています。ホームポジションは `A R S T N E I O` です。

```text
左上:  KC_GRAVE  KC_Q      KC_W      KC_F      KC_P      KC_B      KC_LALT
左中:  LT3(TAB)  KC_A      KC_R      KC_S      KC_T      KC_G      KC_LANG2
左下:  KC_LCTRL  KC_Z      KC_X      KC_C      KC_D      KC_V      -
左親:  -         -         -         KC_ESCAPE LT3(TAB)  LSFT_T(SPACE) -

右上:  KC_RALT   KC_J      KC_L      KC_U      KC_Y      KC_QUOTE  KC_BSLASH
右中:  KC_LANG1  KC_M      KC_N      KC_E      KC_I      KC_O      KC_SCOLON
                  人差し指  人差し指  中指      薬指      小指
右下:  -         KC_K      KC_H      KC_COMMA  KC_DOT    KC_SLASH  KC_RCTRL
右親:  -         RSFT_T(ENTER) LT1(BSPC) KC_RGUI -       -         -
```

### レイヤー 1

数字・記号入力用のレイヤーです。ベースレイヤーの `LT1(KC_BSPACE)` から使う想定です。

```text
左上:  LSFT(`)   LSFT(1)   LSFT(2)   LSFT(3)   LSFT(4)   LSFT(5)   TRNS
左中:  KC_GRAVE  KC_1      KC_2      KC_3      KC_4      KC_5      TRNS
左下:  TRNS      [         LSFT([)   LSFT(,)   =         LSFT(=)   -
左親:  -         -         -         TRNS      RCTL(BSPC) TRNS     -

右上:  TRNS      LSFT(6)   LSFT(7)   LSFT(8)   LSFT(9)   LSFT(0)   LSFT(\)
右中:  TRNS      KC_6      KC_7      KC_8      KC_9      KC_0      KC_BSLASH
右下:  -         LSFT(-)   -         LSFT(.)   LSFT(])   ]         TRNS
右親:  -         TRNS      TRNS      TRNS      -         -         -
```

### レイヤー 2

ファンクションキー、編集、ナビゲーション、メディアキー用のレイヤーです。ベースレイヤーの `LT2(KC_TAB)` から使う想定です。

```text
左上:  ESC       F1        F2        F3        F4        F5        F5
左中:  SPACE     C-a       C-z       END       HOME      DELETE    LANG2
左下:  LCTRL     C-S-z     C-x       C-c       C-v       INSERT    -
左親:  -         -         -         TRNS      TRNS      SPACE     -

右上:  F7        F8        F9        F10       F11       F12       BSPC
右中:  LANG1     LEFT      DOWN      UP        RIGHT     PGUP      PAUSE
右下:  -         MEDIA_PREV MEDIA_STOP MEDIA_PLAY MEDIA_NEXT PGDOWN RCTRL
右親:  -         ENTER     LALT(SPACE) LGUI     -         -         -
```

### レイヤー 3

**リセット済み - 自由にキー配置可能。**

元々は RGB 操作用レイヤーでしたが、リセットして空にしました。LT3(TAB) からアクセスできるため、ecro の拡張キー用に使用します。

```text
左上:  TRNS      NO        NO        NO        NO        NO        NO
左中:  TRNS      RGB_HUI   RGB_SAI   RGB_VAI   RGB_TOG   NO        NO
左下:  TRNS      RGB_HUD   RGB_SAD   RGB_VAD   RGB_MOD   NO        -
左親:  -         -         -         LGUI      TRNS      SPACE     -

右上:  NO        NO        NO        NO        NO        NO        NO
右中:  NO        NO        NO        NO        NO        NO        NO
右下:  NO        NO        NO        NO        NO        NO        -
右親:  -         ENTER     TRNS      RGUI      -         -         -
```

### レイヤー 4

全体が `KC_NO` の未使用レイヤーです。

### レイヤー 5

全体が `KC_TRNS` の透過レイヤーです。

## `encoder_layout`

`encoder_layout` はロータリーエンコーダの割り当てです。

```text
encoder_layout[layer][encoder][direction] = keycode
```

- レイヤー数は 6。
- 各レイヤーには 4 個のエンコーダ設定がある。
- 各エンコーダは `[反時計回り, 時計回り]` の 2 要素。

レイヤー 0 から 3 は同じ RGB 操作です。

```json
[
  ["RGB_MOD", "RGB_RMOD"],
  ["RGB_HUI", "RGB_HUD"],
  ["RGB_VAI", "RGB_VAD"],
  ["RGB_SAI", "RGB_SAD"]
]
```

レイヤー 4 と 5 はすべて透過です。

```json
[
  ["KC_TRNS", "KC_TRNS"],
  ["KC_TRNS", "KC_TRNS"],
  ["KC_TRNS", "KC_TRNS"],
  ["KC_TRNS", "KC_TRNS"]
]
```

## `macro`

`macro` は 16 スロットのマクロ配列です。

```text
macro[index] = macro_definition
```

現在はすべて空配列です。

```json
[[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], []]
```

## `tap_dance`

`tap_dance` は 32 スロットあります。各スロットは 5 要素です。

```text
tap_dance[index] = [
  single_tap,
  single_hold,
  double_tap,
  double_hold,
  tapping_term_ms
]
```

現在は全スロットが未設定で、タッピング判定時間だけ `200` ms です。

```json
["KC_NO", "KC_NO", "KC_NO", "KC_NO", 200]
```

## `combo`

`combo` は 32 スロットあります。各スロットは 5 要素です。

```text
combo[index] = [
  key_1,
  key_2,
  key_3,
  key_4,
  output_key
]
```

現在は全スロットが未設定です。

```json
["KC_NO", "KC_NO", "KC_NO", "KC_NO", "KC_NO"]
```

## `key_override`

`key_override` は 32 スロットあります。各スロットはオブジェクトです。

| フィールド | 内容 |
| --- | --- |
| `trigger` | 置換対象になるキー。 |
| `replacement` | 置換後に送るキー。 |
| `layers` | 対象レイヤーのビットマスク。`65535` は全レイヤー相当。 |
| `trigger_mods` | 発火に必要な修飾キー。 |
| `negative_mod_mask` | 発火を抑止する修飾キー。 |
| `suppressed_mods` | 発火時に抑制する修飾キー。 |
| `options` | Key Override のオプションビット。 |

現在は全スロットが未設定です。

```json
{
  "trigger": "KC_NO",
  "replacement": "KC_NO",
  "layers": 65535,
  "trigger_mods": 0,
  "negative_mod_mask": 0,
  "suppressed_mods": 0,
  "options": 7
}
```

## `settings`

`settings` は配列ではなくオブジェクトです。キーは設定 ID の文字列、値は数値です。

```json
{
  "1": 0,
  "2": 50,
  "3": 0,
  "4": 175,
  "5": 5,
  "6": 5000,
  "7": 200,
  "8": 4,
  "9": 10,
  "10": 20,
  "11": 8,
  "12": 10,
  "13": 30,
  "14": 10,
  "15": 80,
  "16": 8,
  "17": 40,
  "18": 0,
  "19": 80,
  "20": 5,
  "21": 0
}
```
