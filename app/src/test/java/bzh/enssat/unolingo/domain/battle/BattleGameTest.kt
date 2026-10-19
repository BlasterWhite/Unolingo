package bzh.enssat.unolingo.domain.battle

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuestionType
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.domain.quiz.QuestionGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleGameTest {
    private val ka = JapaneseCharacter("か", listOf("ka"))
    private val ki = JapaneseCharacter("き", listOf("ki"))
    private val ku = JapaneseCharacter("く", listOf("ku"))

    // Round 1: find "か"; round 2: read "き".
    private val game = BattleGame(
        listOf(
            Question.RomajiToCharacter(ka, "ka", listOf(ka, ki, ku)),
            Question.CharacterToRomajiChoice(ki, listOf("ka", "ki", "ku")),
        ),
    )

    @Test
    fun firstCorrectAnswer_winsTheRound() {
        val after = game.submit(Player.TWO, "か")

        assertEquals(Player.TWO, after.currentRound.winner)
        assertTrue(after.currentRound.isOver)
        assertEquals(1, after.score(Player.TWO))
        assertEquals(0, after.score(Player.ONE))
    }

    @Test
    fun answerAfterTheRoundIsWon_isIgnored() {
        val won = game.submit(Player.TWO, "か")
        assertSame(won, won.submit(Player.ONE, "か"))
    }

    @Test
    fun wrongAnswer_locksThePlayerButNotTheOpponent() {
        val afterMistake = game.submit(Player.ONE, "き")
        assertFalse(afterMistake.currentRound.isOver)
        assertSame(afterMistake, afterMistake.submit(Player.ONE, "か"))

        val afterOpponent = afterMistake.submit(Player.TWO, "か")
        assertEquals(Player.TWO, afterOpponent.currentRound.winner)
    }

    @Test
    fun bothWrong_endsTheRoundWithoutWinner() {
        val after = game.submit(Player.ONE, "き").submit(Player.TWO, "く")

        assertTrue(after.currentRound.isOver)
        assertNull(after.currentRound.winner)
        assertEquals(0, after.score(Player.ONE) + after.score(Player.TWO))
    }

    @Test(expected = IllegalStateException::class)
    fun nextRound_beforeTheRoundIsOver_fails() {
        game.nextRound()
    }

    @Test
    fun lastRoundOver_finishesTheGameAndDesignatesTheWinner() {
        val finished = game.submit(Player.ONE, "か").nextRound().submit(Player.ONE, "ki")

        assertEquals(2, finished.roundNumber)
        assertTrue(finished.isFinished)
        assertEquals(Player.ONE, finished.winner)
    }

    @Test
    fun equalScores_isADraw() {
        val finished = game.submit(Player.ONE, "か").nextRound().submit(Player.TWO, "ki")

        assertTrue(finished.isFinished)
        assertNull(finished.winner)
    }

    @Test
    fun runningGame_hasNoWinner() {
        assertNull(game.submit(Player.ONE, "か").winner)
    }

    @Test
    fun create_usesOnlyMultipleChoiceQuestions() {
        val characters = "aiueo".map { JapaneseCharacter("$it", listOf("$it")) } +
            listOf("ka", "ki", "ku", "ke", "ko", "sa", "shi").map { JapaneseCharacter(it.uppercase(), listOf(it)) }
        val created = BattleGame.create(characters, QuestionGenerator(Random(4)))

        assertEquals(BattleGame.ROUND_COUNT, created.questions.size)
        assertTrue(created.questions.all { it.type in BattleGame.QUESTION_TYPES })
        assertFalse(created.questions.any { it.type == QuestionType.CHARACTER_TO_ROMAJI })
    }
}
