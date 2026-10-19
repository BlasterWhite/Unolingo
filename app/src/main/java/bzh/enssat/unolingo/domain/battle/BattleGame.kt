package bzh.enssat.unolingo.domain.battle

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuestionType
import bzh.enssat.unolingo.domain.quiz.AnswerChecker
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.domain.quiz.QuestionGenerator

enum class Player {
    ONE,
    TWO;

    val opponent: Player
        get() = if (this == ONE) TWO else ONE
}

/**
 * One question played by both players. The first correct answer wins the round; a wrong answer locks that
 * player out until the next round, and the round ends without a winner when both have answered wrong.
 */
data class BattleRound(
    val question: Question,
    val answers: Map<Player, String> = emptyMap(),
    val winner: Player? = null,
) {
    val isOver: Boolean
        get() = winner != null || answers.size == Player.entries.size
}

/**
 * Immutable battle state, independent of where the answers come from (same device today, Bluetooth or Wi-Fi
 * later): a transport only has to call [submit] and [nextRound].
 */
data class BattleGame(
    val questions: List<Question>,
    /** Played rounds; the last one is the current round. */
    val rounds: List<BattleRound> = listOf(BattleRound(questions.first())),
) {
    init {
        require(rounds.isNotEmpty() && rounds.size <= questions.size) { "Invalid round count" }
    }

    val currentRound: BattleRound
        get() = rounds.last()

    /** 1-based. */
    val roundNumber: Int
        get() = rounds.size

    val isFinished: Boolean
        get() = rounds.size == questions.size && currentRound.isOver

    fun score(player: Player): Int = rounds.count { it.winner == player }

    /** Null while the game is running, and for a draw. */
    val winner: Player?
        get() {
            if (!isFinished) return null
            val difference = score(Player.ONE) - score(Player.TWO)
            return when {
                difference > 0 -> Player.ONE
                difference < 0 -> Player.TWO
                else -> null
            }
        }

    /** Ignored if the round is over or [player] already answered it. */
    fun submit(player: Player, answer: String): BattleGame {
        val round = currentRound
        if (round.isOver || player in round.answers) return this
        val correct = AnswerChecker.isCorrect(round.question, answer)
        val updated = round.copy(answers = round.answers + (player to answer), winner = if (correct) player else null)
        return copy(rounds = rounds.dropLast(1) + updated)
    }

    fun nextRound(): BattleGame {
        check(currentRound.isOver) { "The current round is not over" }
        check(!isFinished) { "The game is finished" }
        return copy(rounds = rounds + BattleRound(questions[rounds.size]))
    }

    companion object {
        const val ROUND_COUNT = 10

        /** Multiple-choice only: both players share one screen, so nobody can type. */
        val QUESTION_TYPES = listOf(QuestionType.ROMAJI_TO_CHARACTER, QuestionType.CHARACTER_TO_ROMAJI_CHOICE)

        fun create(characters: List<JapaneseCharacter>, generator: QuestionGenerator): BattleGame =
            BattleGame(generator.generate(characters, ROUND_COUNT, QUESTION_TYPES))
    }
}
