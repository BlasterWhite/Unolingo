package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizSessionTest {
    private val ka = JapaneseCharacter("か", listOf("ka"))
    private val ki = JapaneseCharacter("き", listOf("ki"))
    private val session = QuizSession(
        listOf(
            Question.CharacterToRomaji(ka),
            Question.RomajiToCharacter(ki, "ki", listOf(ka, ki)),
        ),
    )

    @Test
    fun newSession_startsOnFirstQuestion() {
        assertEquals(session.questions.first(), session.currentQuestion)
        assertEquals(0, session.score)
        assertFalse(session.isFinished)
    }

    @Test
    fun submit_recordsResultsAndAdvances() {
        val afterFirst = session.submit("KA")
        assertTrue(afterFirst.results.single().isCorrect)
        assertEquals(session.questions[1], afterFirst.currentQuestion)

        val finished = afterFirst.submit("か")
        assertFalse(finished.results.last().isCorrect)
        assertEquals(1, finished.score)
        assertTrue(finished.isFinished)
        assertNull(finished.currentQuestion)
    }

    @Test(expected = IllegalStateException::class)
    fun submit_afterLastQuestion_fails() {
        session.submit("ka").submit("き").submit("ka")
    }
}
