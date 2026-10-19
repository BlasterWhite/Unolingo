package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {
    private val shi = JapaneseCharacter("し", listOf("shi"), alternatives = listOf("si"))
    private val sun = JapaneseCharacter("日", listOf("nichi", "jitsu", "hi", "ka"))
    private val mouth = JapaneseCharacter("口", listOf("kou", "ku", "kuchi"))

    @Test
    fun typedReading_isCaseInsensitiveAndTrimmed() {
        assertTrue(AnswerChecker.isCorrect(Question.CharacterToRomaji(shi), "  SHI "))
    }

    @Test
    fun typedReading_acceptsAlternativeSpelling() {
        assertTrue(AnswerChecker.isCorrect(Question.CharacterToRomaji(shi), "si"))
    }

    @Test
    fun typedReading_acceptsAnyKanjiReading() {
        val question = Question.CharacterToRomaji(sun)
        listOf("nichi", "jitsu", "hi", "ka").forEach { assertTrue(it, AnswerChecker.isCorrect(question, it)) }
        assertFalse(AnswerChecker.isCorrect(question, "tsuki"))
    }

    @Test
    fun typedReading_acceptsMacrons() {
        assertTrue(AnswerChecker.isCorrect(Question.CharacterToRomaji(mouth), "kō"))
    }

    @Test
    fun blankAnswer_isWrong() {
        assertFalse(AnswerChecker.isCorrect(Question.CharacterToRomaji(shi), "   "))
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
