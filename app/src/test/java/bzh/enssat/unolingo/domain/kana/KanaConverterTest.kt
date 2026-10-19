package bzh.enssat.unolingo.domain.kana

import bzh.enssat.unolingo.data.AssetCharacterRepository
import bzh.enssat.unolingo.data.CharacterDataParser
import bzh.enssat.unolingo.domain.model.CharacterSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class KanaConverterTest {
    private val converter = KanaConverter(
        CharacterDataParser.parse(
            File("src/main/assets", AssetCharacterRepository.assetPath(CharacterSet.HIRAGANA)).readText(),
        ),
    )

    @Test
    fun singleSyllables() {
        assertEquals("か", converter.toHiragana("ka"))
        assertEquals("し", converter.toHiragana("shi"))
        assertEquals("きゃ", converter.toHiragana("kya"))
        assertEquals("を", converter.toHiragana("wo"))
    }

    @Test
    fun homophones_useTheBasicKana() {
        assertEquals("じ", converter.toHiragana("ji"))
        assertEquals("ず", converter.toHiragana("zu"))
    }

    @Test
    fun kanjiReadings() {
        assertEquals("にち", converter.toHiragana("nichi"))
        assertEquals("きゅう", converter.toHiragana("kyuu"))
        assertEquals("おんな", converter.toHiragana("onna"))
        assertEquals("せん", converter.toHiragana("sen"))
        assertEquals("ひゃく", converter.toHiragana("hyaku"))
    }

    @Test
    fun macronsAndCase_areNormalized() {
        assertEquals("こう", converter.toHiragana("Kō"))
    }

    @Test
    fun unknownSyllable_givesNull() {
        assertNull(converter.toHiragana("xyz"))
        assertNull(converter.toHiragana(""))
    }
}
