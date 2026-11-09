package fr.enssat.unolingo.mguezenn_mphongsa.domain.progress

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeCalculatorTest {
    private val eps = 1e-9

    private fun kana(score: Int, total: Int = 10, help: Boolean = false, at: Long = 0) =
        QuizOutcome(score, total, CharacterSet.HIRAGANA, 0, help, at)

    private fun kanji(score: Int, grade: Int, total: Int = 10, help: Boolean = false, at: Long = 0) =
        QuizOutcome(score, total, CharacterSet.KANJI, grade, help, at)

    @Test fun kanaDifficultyIsOne() = assertEquals(1.0, GradeCalculator.points(kana(10)), eps)

    @Test fun katakanaDifficultyIsOne() =
        assertEquals(0.5, GradeCalculator.points(QuizOutcome(5, 10, CharacterSet.KATAKANA, 3, false, 0)), eps)

    @Test fun kanjiDifficultyByGrade() {
        assertEquals(1.5, GradeConfig.kanjiDifficulty(1), eps)
        assertEquals(4.0, GradeConfig.kanjiDifficulty(6), eps)
        assertEquals(5.0, GradeConfig.kanjiDifficulty(8), eps)
    }

    @Test fun pointsScaleWithScoreRatio() =
        assertEquals(0.5 * 4.0, GradeCalculator.points(kanji(10, 6, total = 20)), eps)

    @Test fun helpMalus() {
        assertEquals(0.8, GradeCalculator.points(kana(10, help = true)), eps)
        assertEquals(0.8 * 4.0, GradeCalculator.points(kanji(10, 6, help = true)), eps)
    }

    @Test fun emptyQuizGivesZero() = assertEquals(0.0, GradeCalculator.points(kana(0, total = 0)), eps)

    @Test fun noQuizGivesZeroValueAndTopKyu() {
        val g = GradeCalculator.grade(emptyList())
        assertEquals(0.0, g.value, eps)
        assertEquals(0, g.rank.level)
        assertEquals(RankType.KYU, g.rank.type)
        assertEquals(10, g.rank.number)
        assertEquals(0.0, g.progress, eps)
    }

    @Test fun fewerThanWindowAveragesExisting() =
        assertEquals(0.75, GradeCalculator.value(listOf(kana(10), kana(5))), eps)

    @Test fun windowDropsOldestOn21stQuiz() {
        val twenty = List(20) { kana(0, at = it.toLong()) }
        assertEquals(0.0, GradeCalculator.value(twenty), eps)
        // The 21st (perfect) quiz pushes the oldest out: 1 perfect out of 20.
        assertEquals(1.0 / 20, GradeCalculator.value(twenty + kana(10, at = 20)), eps)
    }

    @Test fun oldFailuresLeaveTheWindow() {
        val outcomes = List(5) { kana(0, at = it.toLong()) } + List(20) { kana(10, at = 5L + it) }
        assertEquals(1.0, GradeCalculator.value(outcomes), eps)
    }

    @Test fun perfectKanaIsSevenKyu() {
        val g = GradeCalculator.grade(listOf(kana(10)))
        assertEquals(1.0, g.value, eps)
        assertEquals(3, g.rank.level)
        assertEquals(RankType.KYU, g.rank.type)
        assertEquals(7, g.rank.number)
    }

    @Test fun perfectKanjiGrade2IsFourKyu() {
        val g = GradeCalculator.grade(listOf(kanji(10, 2)))
        assertEquals(2.0, g.value, eps)
        assertEquals(6, g.rank.level)
        assertEquals(4, g.rank.number)
        assertEquals(RankType.KYU, g.rank.type)
    }

    @Test fun kanaAloneCapAtSevenKyu() {
        val outcomes = List(30) { kana(10) }
        assertEquals(3, GradeCalculator.grade(outcomes).rank.level)
    }

    @Test fun levelBoundaries() {
        assertEquals(0, GradeCalculator.level(0.0))
        assertEquals(0, GradeCalculator.level(0.33))
        assertEquals(1, GradeCalculator.level(1.0 / 3))
        assertEquals(9, GradeCalculator.level(3.0))
        assertEquals(10, GradeCalculator.level(10.0 / 3))
        assertEquals(14, GradeCalculator.level(5.0 - 1e-9))
    }

    @Test fun kyuAndDanMapping() {
        assertEquals(10, Rank(0).number); assertEquals(RankType.KYU, Rank(0).type)
        assertEquals(1, Rank(9).number); assertEquals(RankType.KYU, Rank(9).type)
        assertEquals(1, Rank(10).number); assertEquals(RankType.DAN, Rank(10).type)
        assertEquals(5, Rank(14).number); assertEquals(RankType.DAN, Rank(14).type)
    }

    @Test fun neverBeyondFiveDan() {
        assertEquals(14, GradeCalculator.level(5.0))
        assertEquals(14, GradeCalculator.level(100.0))
        val g = GradeCalculator.grade(List(20) { kanji(10, 8) }) // value 5
        assertEquals(14, g.rank.level)
        assertEquals(1.0, g.progress, eps)
    }

    @Test fun progressTowardsNextRank() {
        assertEquals(0.0, GradeCalculator.progress(1.0), eps) // start of level 3
        assertEquals(0.5, GradeCalculator.progress(1.0 + 1.0 / 6), 1e-9)
        assertTrue(GradeCalculator.progress(1.0 / 3 - 1e-9) > 0.99)
    }

    @Test fun historyRecomputesAfterEachQuiz() {
        val outcomes = listOf(kana(10, at = 100), kana(0, at = 200), kanji(10, 2, at = 300))
        val h = GradeCalculator.history(outcomes)
        assertEquals(listOf(100L, 200L, 300L), h.map { it.playedAt })
        assertEquals(1.0, h[0].value, eps)
        assertEquals(0.5, h[1].value, eps)
        assertEquals(1.0, h[2].value, eps) // (1 + 0 + 2) / 3
        assertEquals(listOf(3, 1, 3), h.map { it.level })
    }

    @Test fun historyUsesSlidingWindow() {
        val outcomes = List(21) { kana(if (it == 0) 0 else 10, at = it.toLong()) }
        val h = GradeCalculator.history(outcomes)
        assertEquals(19.0 / 20, h[19].value, eps)
        assertEquals(1.0, h[20].value, eps)
    }

    @Test fun historyOfNothingIsEmpty() = assertTrue(GradeCalculator.history(emptyList()).isEmpty())
}
