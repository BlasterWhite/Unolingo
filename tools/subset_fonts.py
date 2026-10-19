"""Copies the app fonts into app/src/main/res/font/, keeping only the Japanese glyphs the app displays.

Klee One covers thousands of kanji (8.9 MB); the app only needs the kana and kanji of its character data, so it
is subset (allowed by the SIL Open Font License, Klee One declares no Reserved Font Name). Lexend is copied as is.
Re-run after adding characters to app/src/main/assets/characters/.

Usage (from the repo root), with the fonts downloaded from https://github.com/google/fonts (ofl/kleeone, ofl/lexend):
    python tools/subset_fonts.py <KleeOne-SemiBold.ttf> <Lexend[wght].ttf>
Requires fontTools (pip install fonttools).
"""
import json
import shutil
import sys
from pathlib import Path

from fontTools import subset

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets/characters"
FONT_DIR = ROOT / "app/src/main/res/font"

# Japanese text shown by the UI outside the character data (logo, player seals, decorations).
UI_TEXT = "うあア日一二"


def japanese_text() -> str:
    chars = set(UI_TEXT)
    for file in sorted(ASSETS.glob("*.json")):
        for character in json.loads(file.read_text(encoding="utf-8"))["characters"]:
            chars.update(character["symbol"])
    return "".join(sorted(chars))


def main(klee_one: str, lexend: str) -> None:
    FONT_DIR.mkdir(parents=True, exist_ok=True)
    text = japanese_text()
    options = subset.Options()
    options.layout_features = ["*"]
    options.hinting = False
    font = subset.load_font(klee_one, options)
    subsetter = subset.Subsetter(options)
    subsetter.populate(text=text)
    subsetter.subset(font)
    target = FONT_DIR / "klee_one_semibold.ttf"
    subset.save_font(font, str(target), options)
    print(target, f"{len(text)} characters, {target.stat().st_size // 1024} KB")

    target = FONT_DIR / "lexend.ttf"
    shutil.copyfile(lexend, target)
    print(target, f"{target.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
