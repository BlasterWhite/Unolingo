package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsUiStateTest {
    private fun outcome(score: Int, at: Long, set: CharacterSet = CharacterSet.HIRAGANA, grade: Int = 1) =
        QuizOutcome(score, 10, set, grade, help = false, playedAt = at)

    @Test
    fun noQuiz_isEmpty_andSummaryIsTenKyu() {
        assertEquals(StatsUiState.Empty, statsUiState(emptyList()))
        val summary = gradeSummary(emptyList())
        assertEquals(0, summary.windowCount)
        assertEquals(0, summary.grade.rank.level)
    }

    @Test
    fun fewQuizzes_oldestLeavesAfterTheWindowFills() {
        val state = statsUiState(List(5) { outcome(10, it.toLong()) }) as StatsUiState.Content
        assertEquals(5, state.summary.windowCount)
        assertEquals(16, state.quizzesBeforeExit)
        assertEquals(5, state.points.size)
    }

    @Test
    fun fullWindow_oldestLeavesAfterOneMoreQuiz() {
        val state = statsUiState(List(25) { outcome(10, it.toLong()) }) as StatsUiState.Content
        assertEquals(20, state.summary.windowCount)
        assertEquals(1, state.quizzesBeforeExit)
        assertEquals(25, state.points.size)
    }

    @Test
    fun recent_isNewestFirst_andBestRankSurvivesADrop() {
        // Kanji grade 8 (difficulty 5) perfect, then zeros: the rank peaks first, then falls.
        val outcomes = listOf(outcome(10, 1, CharacterSet.KANJI, 8)) + List(9) { outcome(0, it + 2L) }
        val state = statsUiState(outcomes) as StatsUiState.Content
        assertEquals(RECENT_QUIZ_COUNT, state.recent.size)
        assertEquals(10L, state.recent.first().playedAt)
        assertTrue(state.bestRank.level > state.summary.grade.rank.level)
        assertEquals(14, state.bestRank.level)
    }
}
