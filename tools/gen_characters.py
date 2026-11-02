"""Generates app/src/main/assets/characters/extras.json: the house additions to the TP data.

The characters themselves (symbols, romaji, kanji readings) come from the TP files in app/src/main/res/raw
(kana_data.json, kanji_jouyou.json). This file only holds what they lack, merged by symbol at load time:
  - "alternatives": other accepted spellings of a kana (si, nn, di…), never displayed;
  - "similar": visually confusable symbols, used to pick distractors.

Usage (from the repo root): python tools/gen_characters.py app/src/main/assets/characters
"""
import json
import sys
from pathlib import Path

out_dir = Path(sys.argv[1])

# (hiragana, readings) — first reading is the Hepburn one of the TP file; the rest become "alternatives".
KANA = [
    # Basic (gojūon)
    ("あ", ["a"]), ("い", ["i"]), ("う", ["u"]), ("え", ["e"]), ("お", ["o"]),
    ("か", ["ka"]), ("き", ["ki"]), ("く", ["ku"]), ("け", ["ke"]), ("こ", ["ko"]),
    ("さ", ["sa"]), ("し", ["shi", "si"]), ("す", ["su"]), ("せ", ["se"]), ("そ", ["so"]),
    ("た", ["ta"]), ("ち", ["chi", "ti"]), ("つ", ["tsu", "tu"]), ("て", ["te"]), ("と", ["to"]),
    ("な", ["na"]), ("に", ["ni"]), ("ぬ", ["nu"]), ("ね", ["ne"]), ("の", ["no"]),
    ("は", ["ha"]), ("ひ", ["hi"]), ("ふ", ["fu", "hu"]), ("へ", ["he"]), ("ほ", ["ho"]),
    ("ま", ["ma"]), ("み", ["mi"]), ("む", ["mu"]), ("め", ["me"]), ("も", ["mo"]),
    ("や", ["ya"]), ("ゆ", ["yu"]), ("よ", ["yo"]),
    ("ら", ["ra"]), ("り", ["ri"]), ("る", ["ru"]), ("れ", ["re"]), ("ろ", ["ro"]),
    ("わ", ["wa"]), ("を", ["wo", "o"]), ("ん", ["n", "nn"]),
    # Dakuten / handakuten
    ("が", ["ga"]), ("ぎ", ["gi"]), ("ぐ", ["gu"]), ("げ", ["ge"]), ("ご", ["go"]),
    ("ざ", ["za"]), ("じ", ["ji", "zi"]), ("ず", ["zu"]), ("ぜ", ["ze"]), ("ぞ", ["zo"]),
    ("だ", ["da"]), ("ぢ", ["ji", "di"]), ("づ", ["zu", "du"]), ("で", ["de"]), ("ど", ["do"]),
    ("ば", ["ba"]), ("び", ["bi"]), ("ぶ", ["bu"]), ("べ", ["be"]), ("ぼ", ["bo"]),
    ("ぱ", ["pa"]), ("ぴ", ["pi"]), ("ぷ", ["pu"]), ("ぺ", ["pe"]), ("ぽ", ["po"]),
    # Yōon
    ("きゃ", ["kya"]), ("きゅ", ["kyu"]), ("きょ", ["kyo"]),
    ("しゃ", ["sha", "sya"]), ("しゅ", ["shu", "syu"]), ("しょ", ["sho", "syo"]),
    ("ちゃ", ["cha", "tya"]), ("ちゅ", ["chu", "tyu"]), ("ちょ", ["cho", "tyo"]),
    ("にゃ", ["nya"]), ("にゅ", ["nyu"]), ("にょ", ["nyo"]),
    ("ひゃ", ["hya"]), ("ひゅ", ["hyu"]), ("ひょ", ["hyo"]),
    ("みゃ", ["mya"]), ("みゅ", ["myu"]), ("みょ", ["myo"]),
    ("りゃ", ["rya"]), ("りゅ", ["ryu"]), ("りょ", ["ryo"]),
    ("ぎゃ", ["gya"]), ("ぎゅ", ["gyu"]), ("ぎょ", ["gyo"]),
    ("じゃ", ["ja", "zya", "jya"]), ("じゅ", ["ju", "zyu", "jyu"]), ("じょ", ["jo", "zyo", "jyo"]),
    ("びゃ", ["bya"]), ("びゅ", ["byu"]), ("びょ", ["byo"]),
    ("ぴゃ", ["pya"]), ("ぴゅ", ["pyu"]), ("ぴょ", ["pyo"]),
]

# Groups of characters that learners commonly confuse visually.
HIRAGANA_SIMILAR = ["ぬめ", "ねれわ", "るろ", "さちき", "はほけ", "いり", "あお", "まも", "こい", "うら", "くへ", "しつ"]
KATAKANA_SIMILAR = ["シツ", "ソンノ", "クタケ", "ヌスフ", "ウワ", "チテ", "コユロ", "ルレ", "ナメ", "ヲラ", "マア", "エユ"]


def to_katakana(text: str) -> str:
    return "".join(chr(ord(c) + 0x60) if "ぁ" <= c <= "ゖ" else c for c in text)


def similar_map(groups):
    result = {}
    for group in groups:
        for c in group:
            result.setdefault(c, []).extend(o for o in group if o != c)
    return result


def kana_extras(convert, groups):
    similar = similar_map(groups)
    extras = {}
    for hira, readings in KANA:
        symbol = convert(hira)
        entry = {}
        # The first reading is the displayed Hepburn one (it is in the TP file); the others are only accepted.
        if len(readings) > 1:
            entry["alternatives"] = readings[1:]
        if symbol in similar:
            entry["similar"] = similar[symbol]
        if entry:
            extras[symbol] = entry
    return extras


KANJI_SIMILAR = ["日目白", "人入", "大天", "木本", "土上", "千十", "未末"]


def kanji_extras():
    return {s: {"similar": o} for s, o in similar_map(KANJI_SIMILAR).items()}


out_dir.mkdir(parents=True, exist_ok=True)
extras = {
    "hiragana": kana_extras(lambda s: s, HIRAGANA_SIMILAR),
    "katakana": kana_extras(to_katakana, KATAKANA_SIMILAR),
    "kanji": kanji_extras(),
}
lines = []
for name, entries in extras.items():
    body = ",\n".join(f"    {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}" for k, v in entries.items())
    lines.append(f'  "{name}": {{\n{body}\n  }}')
path = out_dir / "extras.json"
path.write_text("{\n" + ",\n".join(lines) + "\n}\n", encoding="utf-8", newline="\n")
print(path, {k: len(v) for k, v in extras.items()})
