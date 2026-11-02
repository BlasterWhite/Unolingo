package fr.enssat.unolingo.mguezenn_mphongsa.data

import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaToRomaji
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the real TP files in `src/main/res/raw` (unit tests run with the module directory as working dir). */
class CharacterDataTest {
    private val extras = CharacterExtras.parse(File("src/main/assets", CharacterExtras.ASSET_PATH).readText())
    private val rawKana = parseRawKana(File("src/main/res/raw/kana_data.json").readText())
    private val rawKanji = parseRawKanji(File("src/main/res/raw/kanji_jouyou.json").readText())

    private fun load(set: CharacterSet): List<JapaneseCharacter> = when (set) {
        CharacterSet.HIRAGANA -> CharacterDataParser.mapHiragana(rawKana, extras)
        CharacterSet.KATAKANA -> CharacterDataParser.mapKatakana(rawKana, extras)
        CharacterSet.KANJI -> CharacterDataParser.mapKanji(rawKanji, extras.kanji)
    }

    @Test
    fun everySet_isValid() {
        CharacterSet.entries.forEach { set ->
            val characters = load(set)
            val symbols = characters.map { it.symbol }

            assertTrue("$set is too small for a quiz", characters.size >= 10)
            assertEquals("$set has duplicate symbols", symbols.size, symbols.toSet().size)
            characters.forEach { character ->
                character.acceptedAnswers.forEach {
                    assertTrue("$set ${character.symbol}: bad reading '$it'", it.matches(Regex("[a-z']+")))
                }
                assertTrue("$set ${character.symbol}: unknown similar", symbols.containsAll(character.similar))
            }
        }
    }

    @Test
    fun setSizes() {
        assertEquals(104, load(CharacterSet.HIRAGANA).size)
        assertEquals(104, load(CharacterSet.KATAKANA).size)
        assertEquals(2136, load(CharacterSet.KANJI).size)
    }

    @Test
    fun kana_haveExactlyOneReading_andNoCyrillic() {
        listOf(CharacterSet.HIRAGANA, CharacterSet.KATAKANA).forEach { set ->
            load(set).forEach {
                assertEquals("$set ${it.symbol}", 1, it.readings.size)
                assertFalse("$set ${it.symbol}", it.symbol.contains('я'))
                assertTrue("$set ${it.symbol}: speakable", it.hiraganaFor(it.readings.single()) != null)
            }
        }
        assertEquals("ya", load(CharacterSet.HIRAGANA).single { it.symbol == "や" }.readings.single())
        assertEquals("ya", load(CharacterSet.KATAKANA).single { it.symbol == "ヤ" }.readings.single())
    }

    @Test
    fun everyKanji_hasGradeAndSpeakableReadings() {
        load(CharacterSet.KANJI).forEach { kanji ->
            assertNotNull("${kanji.symbol}: grade", kanji.grade)
            assertTrue("${kanji.symbol}: no reading", kanji.readings.isNotEmpty())
            assertTrue("${kanji.symbol}: no hiragana", kanji.kanaReadings.all.isNotEmpty())
            kanji.kanaReadings.all.forEach {
                assertTrue("${kanji.symbol}: '$it' is not hiragana", it.matches(Regex("[ぁ-ゖー]+")))
            }
            kanji.readings.forEach { reading ->
                val kana = kanji.hiraganaFor(reading)
                assertNotNull("${kanji.symbol}: '$reading' has no hiragana", kana)
                assertEquals("${kanji.symbol}: $reading ↔ $kana", reading, KanaToRomaji.toRomaji(kana!!))
            }
        }
    }

    @Test
    fun hiraganaAndKatakana_haveTheSameReadings() {
        assertEquals(
            load(CharacterSet.HIRAGANA).map { it.acceptedAnswers },
            load(CharacterSet.KATAKANA).map { it.acceptedAnswers },
        )
    }

    @Test
    fun kanaOrder_followsTheFile() {
        assertEquals(
            listOf("あ", "い", "う", "え", "お", "か"),
            load(CharacterSet.HIRAGANA).take(6).map { it.symbol },
        )
    }
}
