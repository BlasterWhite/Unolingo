package fr.enssat.unolingo.mguezenn_mphongsa.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QuizConfigTest {
    @Test
    fun quizHasTenQuestions() {
        assertEquals(10, QuizConfig.QUESTION_COUNT)
    }

    @Test
    fun kana_mixSpreadsBothReadingTypes() {
        assertEquals(
            listOf(QuestionType.CHARACTER_TO_ROMAJI_CHOICE, QuestionType.ROMAJI_TO_CHARACTER),
            QuizConfig(CharacterSet.HIRAGANA).questionTypes,
        )
    }

    @Test
    fun kanji_mixSpreadsTheFourTypes() {
        assertEquals(
            listOf(
                QuestionType.CHARACTER_TO_ROMAJI_CHOICE,
                QuestionType.ROMAJI_TO_CHARACTER,
                QuestionType.CHARACTER_TO_MEANING,
                QuestionType.MEANING_TO_CHARACTER,
            ),
            QuizConfig(CharacterSet.KANJI).questionTypes,
        )
    }

    @Test
    fun tts_turnsReadingQuestionsIntoAudioOnes() {
        assertEquals(
            listOf(QuestionType.CHARACTER_TO_SOUND, QuestionType.SOUND_TO_CHARACTER),
            QuizConfig(CharacterSet.KATAKANA, tts = true).questionTypes,
        )
        assertEquals(
            listOf(QuestionType.CHARACTER_TO_SOUND),
            QuizConfig(CharacterSet.KANJI, QuestionKind.CHARACTER_TO_READING, tts = true).questionTypes,
        )
    }

    @Test
    fun tts_keepsMeaningQuestionsVisual() {
        assertEquals(
            listOf(QuestionType.CHARACTER_TO_MEANING),
            QuizConfig(CharacterSet.KANJI, QuestionKind.CHARACTER_TO_MEANING, tts = true).questionTypes,
        )
        assertEquals(
            listOf(QuestionType.MEANING_TO_CHARACTER),
            QuizConfig(CharacterSet.KANJI, QuestionKind.MEANING_TO_CHARACTER, tts = true).questionTypes,
        )
    }

    @Test
    fun meaningQuestions_areForKanjiOnly() {
        assertThrows(IllegalArgumentException::class.java) {
            QuizConfig(CharacterSet.HIRAGANA, QuestionKind.CHARACTER_TO_MEANING)
        }
        assertEquals(
            listOf(QuestionKind.CHARACTER_TO_READING, QuestionKind.READING_TO_CHARACTER, QuestionKind.MIX),
            QuestionKind.availableFor(CharacterSet.KATAKANA),
        )
    }

    @Test
    fun grade_defaultsToOneForKanaAndTwoForKanji_andMustExist() {
        assertEquals(1, QuizConfig(CharacterSet.HIRAGANA).grade)
        assertEquals(2, QuizConfig(CharacterSet.KANJI).grade)
        assertEquals(8, QuizConfig(CharacterSet.KANJI, grade = 8).grade)
        assertThrows(IllegalArgumentException::class.java) { QuizConfig(CharacterSet.KANJI, grade = 7) }
        assertThrows(IllegalArgumentException::class.java) { QuizConfig(CharacterSet.HIRAGANA, grade = 3) }
    }

    @Test
    fun kanji_isTheOnlyIdeographicSet() {
        assertEquals(
            listOf(CharacterSet.KANJI),
            CharacterSet.entries.filter { it.family == CharacterFamily.IDEOGRAPHIC },
        )
    }
}
