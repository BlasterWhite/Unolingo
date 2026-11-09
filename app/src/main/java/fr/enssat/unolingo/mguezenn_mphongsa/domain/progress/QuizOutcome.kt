package fr.enssat.unolingo.mguezenn_mphongsa.domain.progress

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet

/**
 * Result of one finished quiz, as seen by the grade computation.
 *
 * @param grade kanji grade of the quiz (only meaningful when [characterSet] is KANJI).
 * @param help whether the help was enabled during the quiz.
 * @param playedAt epoch milliseconds.
 */
data class QuizOutcome(
    val score: Int,
    val total: Int,
    val characterSet: CharacterSet,
    val grade: Int,
    val help: Boolean,
    val playedAt: Long,
)
