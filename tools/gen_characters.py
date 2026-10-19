"""Generates app/src/main/assets/characters/{hiragana,katakana,kanji}.json.

Usage (from the repo root): python tools/gen_characters.py app/src/main/assets/characters
"""
import json
import sys
from pathlib import Path

out_dir = Path(sys.argv[1])

# (hiragana, readings) — first reading is the Hepburn one shown to the user.
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


def kana_entries(convert, groups):
    similar = similar_map(groups)
    entries = []
    for hira, readings in KANA:
        symbol = convert(hira)
        # A kana has one displayed reading; other spellings are only accepted as answers.
        entry = {"symbol": symbol, "readings": readings[:1]}
        if len(readings) > 1:
            entry["alternatives"] = readings[1:]
        if symbol in similar:
            entry["similar"] = similar[symbol]
        entries.append(entry)
    return entries


# (kanji, readings, French meaning) — JLPT N5 basics, readings in wāpuro rōmaji (long vowels as ou/uu).
KANJI = [
    ("一", ["ichi", "itsu", "hito"], "un"),
    ("二", ["ni", "futa"], "deux"),
    ("三", ["san", "mi"], "trois"),
    ("四", ["shi", "yon", "yo"], "quatre"),
    ("五", ["go", "itsu"], "cinq"),
    ("六", ["roku", "mu"], "six"),
    ("七", ["shichi", "nana"], "sept"),
    ("八", ["hachi", "ya"], "huit"),
    ("九", ["kyuu", "ku", "kokono"], "neuf"),
    ("十", ["juu", "too", "to"], "dix"),
    ("百", ["hyaku"], "cent"),
    ("千", ["sen", "chi"], "mille"),
    ("万", ["man", "ban"], "dix mille"),
    ("円", ["en", "maru"], "yen, cercle"),
    ("日", ["nichi", "jitsu", "hi", "ka"], "jour, soleil"),
    ("月", ["getsu", "gatsu", "tsuki"], "lune, mois"),
    ("火", ["ka", "hi"], "feu"),
    ("水", ["sui", "mizu"], "eau"),
    ("木", ["moku", "boku", "ki"], "arbre"),
    ("金", ["kin", "kon", "kane"], "or, argent"),
    ("土", ["do", "to", "tsuchi"], "terre"),
    ("人", ["jin", "nin", "hito"], "personne"),
    ("山", ["san", "yama"], "montagne"),
    ("川", ["sen", "kawa"], "rivière"),
    ("口", ["kou", "ku", "kuchi"], "bouche"),
    ("目", ["moku", "me"], "œil"),
    ("手", ["shu", "te"], "main"),
    ("足", ["soku", "ashi"], "pied"),
    ("大", ["dai", "tai", "oo"], "grand"),
    ("小", ["shou", "chii", "ko"], "petit"),
    ("中", ["chuu", "naka"], "milieu, dedans"),
    ("上", ["jou", "ue", "kami"], "dessus"),
    ("下", ["ka", "ge", "shita"], "dessous"),
    ("年", ["nen", "toshi"], "année"),
    ("学", ["gaku", "mana"], "étudier"),
    ("生", ["sei", "shou", "nama"], "vie, naître"),
    ("先", ["sen", "saki"], "avant, précédent"),
    ("時", ["ji", "toki"], "temps, heure"),
    ("本", ["hon", "moto"], "livre, origine"),
    ("男", ["dan", "nan", "otoko"], "homme"),
    ("女", ["jo", "nyo", "onna"], "femme"),
    ("子", ["shi", "su", "ko"], "enfant"),
    ("白", ["haku", "byaku", "shiro"], "blanc"),
    ("天", ["ten", "ama"], "ciel"),
    ("雨", ["u", "ame"], "pluie"),
    ("花", ["ka", "hana"], "fleur"),
    ("見", ["ken", "mi"], "voir"),
    ("行", ["kou", "gyou", "i"], "aller"),
    ("来", ["rai", "ku", "ki"], "venir"),
]
KANJI_SIMILAR = ["日目白", "人入", "大天", "木本", "土上", "千十", "未末"]


def kanji_entries():
    similar = similar_map(KANJI_SIMILAR)
    symbols = {k for k, _, _ in KANJI}
    entries = []
    for symbol, readings, meaning in KANJI:
        entry = {"symbol": symbol, "readings": readings, "meaning": meaning}
        sims = [s for s in similar.get(symbol, []) if s in symbols]
        if sims:
            entry["similar"] = sims
        entries.append(entry)
    return entries


def write(name, entries):
    path = out_dir / f"{name}.json"
    lines = ",\n".join("    " + json.dumps(e, ensure_ascii=False) for e in entries)
    path.write_text('{\n  "characters": [\n' + lines + "\n  ]\n}\n", encoding="utf-8", newline="\n")
    print(path, len(entries))


out_dir.mkdir(parents=True, exist_ok=True)
write("hiragana", kana_entries(lambda s: s, HIRAGANA_SIMILAR))
write("katakana", kana_entries(to_katakana, KATAKANA_SIMILAR))
write("kanji", kanji_entries())
