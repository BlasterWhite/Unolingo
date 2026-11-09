package fr.enssat.unolingo.mguezenn_mphongsa.domain.progress

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import kotlin.math.floor
import kotlin.math.min

/** Kyū (beginner, 10 → 1) or dan (advanced, 1 → 5) rank. */
enum class RankType { KYU, DAN }

/** A rank on the 15-level scale: [level] 0..9 = 10 kyū..1 kyū, 10..14 = 1 dan..5 dan. */
data class Rank(val level: Int) {
    val type: RankType get() = if (level < GradeConfig.KYU_COUNT) RankType.KYU else RankType.DAN

    /** The number shown with the rank (10 kyū → 1 kyū, then 1 dan → 5 dan). */
    val number: Int
        get() = if (level < GradeConfig.KYU_COUNT) GradeConfig.KYU_COUNT - level else level - GradeConfig.KYU_COUNT + 1
}

/** The player's current grade. [progress] (0..1) is the way to the next rank, 1 at the top rank. */
data class Grade(val value: Double, val rank: Rank, val progress: Double)

/** One point of the grade chart: value and level recomputed after the quiz played at [playedAt]. */
data class GradePoint(val playedAt: Long, val value: Double, val level: Int)

/** Pure grade computation over a sliding window of the latest quizzes. */
object GradeCalculator {

    fun difficulty(characterSet: CharacterSet, grade: Int): Double =
        if (characterSet == CharacterSet.KANJI) GradeConfig.kanjiDifficulty(grade) else GradeConfig.KANA_DIFFICULTY

    /** Points of one quiz: score / total × difficulty × help factor (0 for an empty quiz). */
    fun points(outcome: QuizOutcome): Double {
        if (outcome.total <= 0) return 0.0
        val help = if (outcome.help) GradeConfig.HELP_FACTOR else 1.0
        return outcome.score.toDouble() / outcome.total * difficulty(outcome.characterSet, outcome.grade) * help
    }

    /** Average points of the last [GradeConfig.WINDOW_SIZE] quizzes of a chronological list; 0 if empty. */
    fun value(outcomes: List<QuizOutcome>): Double {
        val window = outcomes.takeLast(GradeConfig.WINDOW_SIZE)
        if (window.isEmpty()) return 0.0
        return window.sumOf { points(it) } / window.size
    }

    /** Rank level 0..14: min(14, floor(value / 5 × 15)). */
    fun level(value: Double): Int {
        val scaled = value * GradeConfig.LEVEL_COUNT / GradeConfig.MAX_VALUE
        return min(GradeConfig.LEVEL_COUNT - 1, floor(scaled).toInt()).coerceAtLeast(0)
    }

    /** Progress 0..1 towards the next level; 1 once the top level is reached. */
    fun progress(value: Double): Double {
        val scaled = value * GradeConfig.LEVEL_COUNT / GradeConfig.MAX_VALUE
        val level = level(value)
        if (level >= GradeConfig.LEVEL_COUNT - 1) return 1.0
        return (scaled - level).coerceIn(0.0, 1.0)
    }

    fun grade(outcomes: List<QuizOutcome>): Grade {
        val value = value(outcomes)
        return Grade(value, Rank(level(value)), progress(value))
    }

    /** Value and level after each quiz, each computed on the window ending at that quiz. */
    fun history(outcomes: List<QuizOutcome>): List<GradePoint> =
        outcomes.indices.map { i ->
            val value = value(outcomes.subList(0, i + 1))
            GradePoint(outcomes[i].playedAt, value, level(value))
        }
}
