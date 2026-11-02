package fr.enssat.unolingo.mguezenn_mphongsa.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDataParserTest {
    private val kanaJson = """
        {"hiragana": {"hiragana_alphabet": {"voyelles": [{"char":"あ","romaji":"a"}],
                                            "line_Y": [{"char":"я","romaji":"ya"},{"char":"ゆ","romaji":"yu"}]},
                      "yoon_combinaisons": {"base_K": [{"char":"きゃ","romaji":"kya"}]}},
         "katakana": {"katakana_alphabet": {"voyelles": [{"char":"ア","romaji":"a"}],
                                            "line_Y": [{"char":"я","romaji":"ya"}]}}}
    """.trimIndent()

    private val kanjiJson = """
        {"一": {"strokes":1,"grade":1,"freq":2,"jlpt_new":5,"meanings":["One","One Radical"],
                "readings_on":["いち","イツ"],"readings_kun":["ひと-","ひと.つ"],"wk_level":1,"wk_radicals":["Ground"]},
         "日": {"strokes":4,"grade":1,"meanings":["Day"],"readings_on":["にち","じつ"],
                "readings_kun":["ひ","-び","か","-か","じつ"]},
         "学": {"grade":1,"readings_on":["ガク","ガッ"],"readings_kun":["まな.ぶ"]},
         "凸": {"grade":8,"readings_on":["かっ"],"readings_kun":["っこ", "で.っ"]}}
    """.trimIndent()

    @Test
    fun parsesRawKana_inFileOrder() {
        val raw = parseRawKana(kanaJson)
        assertEquals(listOf("あ", "я", "ゆ", "きゃ"), raw.hiragana.values.flatMap { it.values.flatten() }.map { it.char })
    }

    @Test
    fun mapsKana_fixingCyrillicYa() {
        val raw = parseRawKana(kanaJson)
        val hiragana = CharacterDataParser.mapHiragana(raw)
        assertEquals(listOf("あ", "や", "ゆ", "きゃ"), hiragana.map { it.symbol })
        assertEquals(listOf("a", "ya", "yu", "kya"), hiragana.map { it.readings.single() })
        assertEquals(listOf("ア", "ヤ"), CharacterDataParser.mapKatakana(raw).map { it.symbol })
    }

    @Test
    fun kana_mergeExtrasBySymbol() {
        val extras = CharacterExtras.parse("""{"hiragana": {"あ": {"alternatives": ["aa"], "similar": ["ゆ"]}}}""")
        val a = CharacterDataParser.mapHiragana(parseRawKana(kanaJson), extras).first()
        assertEquals(listOf("a", "aa"), a.acceptedAnswers)
        assertEquals(setOf("ゆ"), a.similar)
        assertEquals("あ", a.hiraganaFor("a"))
        assertEquals("あ", CharacterDataParser.mapKatakana(parseRawKana(kanaJson)).first().hiraganaFor("a"))
    }

    @Test
    fun parsesRawKanji_ignoringUnknownKeys() {
        val raw = parseRawKanji(kanjiJson)
        assertEquals(4, raw.size)
        assertEquals(1, raw.getValue("一").grade)
        assertEquals(listOf("ひと-", "ひと.つ"), raw.getValue("一").readings_kun)
    }

    private fun kanji(symbol: String) =
        CharacterDataParser.mapKanji(parseRawKanji(kanjiJson)).single { it.symbol == symbol }

    @Test
    fun kanji_cleansKunReadings_andConvertsKatakanaOn() {
        val ichi = kanji("一")
        assertEquals(1, ichi.grade)
        assertEquals(listOf("One", "One Radical"), ichi.meaningsEn)
        assertEquals(listOf("ichi", "itsu", "hito"), ichi.readings) // イツ → いつ, ひと- and ひと.つ → ひと (duplicate removed)
        assertEquals(listOf("いち", "いつ"), ichi.kanaReadings.on)
        assertEquals(listOf("ひと"), ichi.kanaReadings.kun)
        assertEquals(listOf("hitotsu"), ichi.alternatives) // full form with okurigana: accepted, not displayed
        assertEquals("いつ", ichi.hiraganaFor("itsu"))
        assertEquals("ひと", ichi.hiraganaFor("Hito"))
    }

    @Test
    fun kanji_removesDuplicatesAcrossOnAndKun() {
        val nichi = kanji("日")
        assertEquals(listOf("nichi", "jitsu", "hi", "bi", "ka"), nichi.readings) // じつ (kun) = じつ (on): once; ka once
        assertEquals(listOf("じつ"), nichi.kanaReadings.on.drop(1))
        assertTrue(nichi.kanaReadings.kun.none { it == "じつ" })
    }

    @Test
    fun kanji_discardsUnconvertibleReadings() {
        val gaku = kanji("学")
        assertEquals(listOf("gaku", "mana"), gaku.readings) // ガッ discarded
        assertEquals(listOf("ガク").map { "がく" }, gaku.kanaReadings.on)
        assertEquals(listOf("manabu"), gaku.alternatives)
    }

    @Test
    fun kanji_alwaysKeepsOneReading() {
        // かっ and っこ cannot be converted, and the stem of で.っ is "で": the kanji keeps it.
        val totsu = kanji("凸")
        assertEquals(listOf("de"), totsu.readings)
        assertEquals("で", totsu.hiraganaFor("de"))
    }
}
