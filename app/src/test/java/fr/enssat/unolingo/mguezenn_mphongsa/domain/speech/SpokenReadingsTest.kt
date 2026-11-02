package fr.enssat.unolingo.mguezenn_mphongsa.domain.speech

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.KanaReadings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpokenReadingsTest {
    private val shi = JapaneseCharacter(symbol = "し", readings = listOf("shi"))
    private val ka = JapaneseCharacter(symbol = "カ", readings = listOf("ka"))
    private val sun = JapaneseCharacter(
        symbol = "日",
        readings = listOf("nichi", "hi"),
        kanaReadings = KanaReadings(on = listOf("にち"), kun = listOf("ひ")),
    )
    private val fire = JapaneseCharacter(
        symbol = "火",
        readings = listOf("ka", "hi"),
        kanaReadings = KanaReadings(on = listOf("か"), kun = listOf("ひ")),
    )

    @Test
    fun eachKanaReading_isSpokenAsItsOwnCharacter() {
        val spoken = SpokenReadings(listOf(shi, ka))

        // A "character → sound" question on し offers "ka" as a wrong proposal: it must sound like か, not し.
        assertEquals("し", spoken.hiraganaFor("shi"))
        assertEquals("か", spoken.hiraganaFor("ka"))
    }

    @Test
    fun kanjiReadings_useTheHiraganaOfTheData() {
        val spoken = SpokenReadings(listOf(sun, fire))

        assertEquals("にち", spoken.hiraganaFor("nichi"))
        assertEquals("か", spoken.hiraganaFor("ka"))
        assertEquals("ひ", spoken.hiraganaFor("hi"))
    }

    @Test
    fun lookup_ignoresCaseAndSpaces() {
        assertEquals("にち", SpokenReadings(listOf(sun)).hiraganaFor(" Nichi "))
    }

    @Test
    fun unknownReading_isNull() {
        assertNull(SpokenReadings(listOf(shi)).hiraganaFor("ka"))
    }
}
