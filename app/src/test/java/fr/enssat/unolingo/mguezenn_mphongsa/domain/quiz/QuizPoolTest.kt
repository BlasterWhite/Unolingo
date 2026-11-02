package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizPoolTest {
    private val kanji = (1..8).flatMap { grade ->
        (0 until 12).map { i ->
            JapaneseCharacter(symbol = "g$grade-$i", readings = listOf("r$grade$i"), grade = grade)
        }
    }

    @Test
    fun kanji_defaultKeepsGradesOneAndTwo() {
        val pool = QuizPool.select(CharacterSet.KANJI, kanji)

        assertEquals(24, pool.size)
        assertTrue(pool.all { it.grade in setOf(1, 2) })
    }

    @Test
    fun kanji_customGrades() {
        val pool = QuizPool.select(CharacterSet.KANJI, kanji, setOf(3))

        assertEquals(12, pool.size)
        assertTrue(pool.all { it.grade == 3 })
    }

    @Test
    fun kanji_gradesMatchingNothing_fallBackToAll() {
        assertEquals(kanji, QuizPool.select(CharacterSet.KANJI, kanji, setOf(9)))
    }

    @Test
    fun kana_areNeverFiltered() {
        val kana = listOf(JapaneseCharacter("あ", listOf("a")), JapaneseCharacter("い", listOf("i")))

        assertEquals(kana, QuizPool.select(CharacterSet.HIRAGANA, kana))
    }

    @Test
    fun questionsAndDistractors_comeFromTheSameGrades() {
        val pool = QuizPool.select(CharacterSet.KANJI, kanji)
        val allowed = pool.map { it.symbol }.toSet()
        val readings = pool.flatMap { it.readings }.toSet()

        repeat(20) { seed ->
            val questions = QuestionGenerator(Random(seed)).generate(pool, QuizMode.DISPLAY)
            questions.forEach { q ->
                assertTrue(q.character.symbol in allowed)
                when (q) {
                    is Question.RomajiToCharacter -> assertTrue(q.choices.all { it.symbol in allowed })
                    is Question.CharacterToRomajiChoice -> assertTrue(q.choices.all { it in readings })
                    else -> Unit
                }
            }
        }
    }
}
