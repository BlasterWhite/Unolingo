package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.Grade
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradeCalculator
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradeConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradePoint
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.Rank
import kotlin.math.min

/** Current grade and how many quizzes currently count for it (shown on the home card and the progress page). */
data class GradeSummary(val grade: Grade, val windowCount: Int)

/** One line of the short history: a played quiz and the rank the player had right after it. */
data class HistoryRow(
    val playedAt: Long,
    val characterSet: CharacterSet,
    /** Kanji grade of the quiz (only meaningful for [CharacterSet.KANJI]). */
    val kanjiGrade: Int,
    val score: Int,
    val total: Int,
    val rank: Rank,
)

sealed interface StatsUiState {
    data object Loading : StatsUiState

    /** No quiz played yet. */
    data object Empty : StatsUiState

    data class Content(
        val summary: GradeSummary,
        /** Grade after each quiz, oldest first. */
        val points: List<GradePoint>,
        val bestRank: Rank,
        /** Quizzes to play before the oldest quiz of the window stops counting. */
        val quizzesBeforeExit: Int,
        /** Latest quizzes, newest first. */
        val recent: List<HistoryRow>,
    ) : StatsUiState
}

const val RECENT_QUIZ_COUNT = 8

/** Grade and window size for [outcomes] (chronological); a player without quiz has 10 kyū and an empty window. */
fun gradeSummary(outcomes: List<QuizOutcome>): GradeSummary =
    GradeSummary(GradeCalculator.grade(outcomes), min(outcomes.size, GradeConfig.WINDOW_SIZE))

/** Builds the progress page state from all outcomes, oldest first. */
fun statsUiState(outcomes: List<QuizOutcome>): StatsUiState {
    if (outcomes.isEmpty()) return StatsUiState.Empty
    val points = GradeCalculator.history(outcomes)
    val summary = gradeSummary(outcomes)
    return StatsUiState.Content(
        summary = summary,
        points = points,
        bestRank = Rank(points.maxOf { it.level }),
        quizzesBeforeExit = GradeConfig.WINDOW_SIZE - summary.windowCount + 1,
        recent = outcomes.indices.reversed().take(RECENT_QUIZ_COUNT).map { i ->
            val outcome = outcomes[i]
            HistoryRow(outcome.playedAt, outcome.characterSet, outcome.grade, outcome.score, outcome.total, Rank(points[i].level))
        },
    )
}
