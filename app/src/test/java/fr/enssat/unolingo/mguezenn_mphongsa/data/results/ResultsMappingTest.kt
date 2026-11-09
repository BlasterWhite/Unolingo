package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsMappingTest {
    private val outcome = QuizOutcome(
        score = 7, total = 10, characterSet = CharacterSet.KANJI, grade = 3, help = true, playedAt = 1_700_000_000_000,
    )

    @Test
    fun entityToOutcome_copiesEveryField() {
        val entity = QuizResultEntity(
            id = 5, playedAt = 42, characterSet = CharacterSet.KATAKANA, questionKind = QuestionKind.READING_TO_CHARACTER,
            help = true, tts = true, grade = 1, score = 9, total = 10,
        )
        assertEquals(QuizOutcome(9, 10, CharacterSet.KATAKANA, 1, true, 42), entity.toOutcome())
    }

    @Test
    fun outcomeToFakeEntity_isFlaggedFakeAndRoundTrips() {
        val entity = outcome.toFakeEntity()
        assertTrue(entity.isFake)
        assertEquals(0L, entity.id)
        assertEquals(outcome, entity.toOutcome())
    }

    @Test
    fun entity_isNotFakeByDefault() {
        val entity = QuizResultEntity(
            playedAt = 1, characterSet = CharacterSet.HIRAGANA, questionKind = QuestionKind.MIX,
            help = false, tts = false, grade = 1, score = 1, total = 10,
        )
        assertFalse(entity.isFake)
    }
}
