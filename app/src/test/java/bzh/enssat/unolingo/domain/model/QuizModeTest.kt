package bzh.enssat.unolingo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QuizModeTest {
    @Test
    fun displayMode_hasTenVisualQuestions() {
        assertEquals(10, QuizMode.DISPLAY.questionCount)
        assertEquals(
            listOf(QuestionType.CHARACTER_TO_ROMAJI, QuestionType.ROMAJI_TO_CHARACTER),
            QuizMode.DISPLAY.questionTypes,
        )
    }

    @Test
    fun audioMode_hasTwentyAudioQuestions() {
        assertEquals(20, QuizMode.AUDIO.questionCount)
        assertEquals(
            listOf(QuestionType.SOUND_TO_CHARACTER, QuestionType.CHARACTER_TO_SOUND),
            QuizMode.AUDIO.questionTypes,
        )
    }

    @Test
    fun kanji_isTheOnlyIdeographicSet() {
        assertEquals(
            listOf(CharacterSet.KANJI),
            CharacterSet.entries.filter { it.family == CharacterFamily.IDEOGRAPHIC },
        )
    }
}
