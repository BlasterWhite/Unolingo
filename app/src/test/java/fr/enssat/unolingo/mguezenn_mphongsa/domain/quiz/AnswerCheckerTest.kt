package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {
    private val shi = JapaneseCharacter("し", listOf("shi"), alternatives = listOf("si"))
    private val sun = JapaneseCharacter("日", listOf("nichi", "jitsu", "hi", "ka"))
    private val mouth = JapaneseCharacter("口", listOf("kou", "ku", "kuchi"))

    @Test
    fun reading_isCaseInsensitiveAndTrimmed() {
        assertTrue(AnswerChecker.isCorrect(Question.CharacterToRomajiChoice(shi, listOf("shi", "tsu")), "  SHI "))
    }

    @Test
    fun reading_acceptsAlternativeSpelling() {
        assertTrue(AnswerChecker.isCorrect(Question.CharacterToRomajiChoice(shi, listOf("shi", "tsu")), "si"))
    }

    @Test
    fun reading_acceptsAnyKanjiReading() {
        val question = Question.CharacterToRomajiChoice(sun, listOf("hi", "tsuki"))
        listOf("nichi", "jitsu", "hi", "ka").forEach { assertTrue(it, AnswerChecker.isCorrect(question, it)) }
        assertFalse(AnswerChecker.isCorrect(question, "tsuki"))
    }

    @Test
    fun reading_acceptsMacrons() {
        assertTrue(AnswerChecker.isCorrect(Question.CharacterToRomajiChoice(mouth, listOf("kou", "tsuki")), "kō"))
    }

    @Test
    fun blankAnswer_isWrong() {
        assertFalse(AnswerChecker.isCorrect(Question.CharacterToRomajiChoice(shi, listOf("shi", "tsu")), "   "))
    }

    @Test
    fun reading_inHiraganaIsTheSameAnswerAsItsRomaji() {
        val question = Question.CharacterToRomajiChoice(mouth, listOf("こう", "つき"))
        assertTrue(AnswerChecker.isCorrect(question, "こう"))
        assertTrue(AnswerChecker.isCorrect(question, "くち"))
        assertFalse(AnswerChecker.isCorrect(question, "つき"))
        assertEquals("kyouto", AnswerChecker.normalizeRomaji("きょうと"))
    }

    @Test
    fun meaningQuestions_areCorrectOnlyForTheTargetKanji() {
        val water = JapaneseCharacter("水", listOf("sui"), meaning = "water")
        val fire = JapaneseCharacter("火", listOf("ka"), meaning = "fire")
        val toMeaning = Question.CharacterToMeaning(water, listOf("water", "fire"))
        assertTrue(AnswerChecker.isCorrect(toMeaning, "water"))
        assertFalse(AnswerChecker.isCorrect(toMeaning, "fire"))
        val toCharacter = Question.MeaningToCharacter(water, "water", listOf(water, fire))
        assertTrue(AnswerChecker.isCorrect(toCharacter, "水"))
        assertFalse(AnswerChecker.isCorrect(toCharacter, "火"))
    }

    @Test
    fun choice_isCorrectOnlyForTheTargetSymbol() {
        val tsu = JapaneseCharacter("つ", listOf("tsu"))
        val question = Question.RomajiToCharacter(shi, "shi", listOf(shi, tsu))
        assertTrue(AnswerChecker.isCorrect(question, "し"))
        assertFalse(AnswerChecker.isCorrect(question, "つ"))
    }

    @Test
    fun normalizeRomaji_removesSeparatorsAndExpandsLongVowels() {
        assertEquals("kyouto", AnswerChecker.normalizeRomaji("Kyō-to"))
        assertEquals("konnichiha", AnswerChecker.normalizeRomaji("kon'nichi ha"))
    }

    @Test
    fun soundToCharacter_isCorrectOnlyForTheTargetSymbol() {
        val tsu = JapaneseCharacter("つ", listOf("tsu"))
        val question = Question.SoundToCharacter(shi, "shi", listOf(shi, tsu))
        assertTrue(AnswerChecker.isCorrect(question, "し"))
        assertFalse(AnswerChecker.isCorrect(question, "つ"))
    }

    @Test
    fun characterToSound_acceptsAnyReadingOfTheCharacter() {
        val question = Question.CharacterToSound(sun, listOf("hi", "tsuki", "mizu", "yama"))
        assertTrue(AnswerChecker.isCorrect(question, "hi"))
        assertFalse(AnswerChecker.isCorrect(question, "tsuki"))
    }
}
