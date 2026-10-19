package bzh.enssat.unolingo.data

import bzh.enssat.unolingo.domain.kana.KanaConverter
import bzh.enssat.unolingo.domain.model.CharacterFamily
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the real files in `src/main/assets` (unit tests run with the module directory as working dir). */
class CharacterDataTest {
    private fun load(set: CharacterSet): List<JapaneseCharacter> =
        CharacterDataParser.parse(File("src/main/assets", AssetCharacterRepository.assetPath(set)).readText())

    @Test
    fun everySet_isValid() {
        CharacterSet.entries.forEach { set ->
            val characters = load(set)
            val symbols = characters.map { it.symbol }

            assertTrue("$set is too small for a quiz", characters.size >= 10)
            assertEquals("$set has duplicate symbols", symbols.size, symbols.toSet().size)
            characters.forEach { character ->
                character.acceptedAnswers.forEach {
                    assertTrue("$set ${character.symbol}: bad reading '$it'", it.matches(Regex("[a-z]+")))
                }
                assertTrue("$set ${character.symbol}: unknown similar", symbols.containsAll(character.similar))
                assertEquals(
                    "$set ${character.symbol}: meaning",
                    set.family == CharacterFamily.IDEOGRAPHIC,
                    character.meaning != null,
                )
            }
        }
    }

    @Test
    fun kana_haveExactlyOneDisplayedReading() {
        listOf(CharacterSet.HIRAGANA, CharacterSet.KATAKANA).forEach { set ->
            load(set).forEach { assertEquals("$set ${it.symbol}", 1, it.readings.size) }
        }
    }

    @Test
    fun everyReading_canBeSpoken() {
        // Audio questions send readings to text-to-speech written in hiragana.
        val converter = KanaConverter(load(CharacterSet.HIRAGANA))
        CharacterSet.entries.forEach { set ->
            load(set).forEach { character ->
                character.readings.forEach {
                    assertTrue("$set ${character.symbol}: '$it' has no hiragana", converter.toHiragana(it) != null)
                }
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
}
