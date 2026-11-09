package fr.enssat.unolingo.mguezenn_mphongsa

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.ResultsRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.UnolingoDatabase
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import kotlin.math.floor
import kotlin.math.min

/** Helpers shared by the instrumented statistics tests. They work on the real database of the installed app. */
object TestSupport {
    /** `total` of the real "witness" result: unusual, so a witness left by a crashed run is recognized and purged. */
    const val WITNESS_TOTAL = 4242

    const val FAKE_QUIZ_COUNT = 25

    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as UnolingoApplication

    val repository: ResultsRepository get() = app.container.resultsRepository

    /**
     * 25 known fake quizzes mixing hiragana, katakana and kanji of grades 1..8, with and without help, played one
     * minute apart starting at [firstPlayedAt]. The 5 oldest are perfect kanji grade 8 quizzes: they fall outside
     * the 20-quiz window, so a computation that ignored the window would give a different rank.
     */
    fun fakeOutcomes(firstPlayedAt: Long): List<QuizOutcome> = List(FAKE_QUIZ_COUNT) { i ->
        val playedAt = firstPlayedAt + i * 60_000L
        if (i < 5) {
            QuizOutcome(10, 10, CharacterSet.KANJI, 8, help = false, playedAt = playedAt)
        } else {
            val set = CharacterSet.entries[i % 3]
            val grade = if (set == CharacterSet.KANJI) listOf(1, 2, 3, 4, 5, 6, 8)[i % 7] else 1
            QuizOutcome(4 + (i * 3) % 7, 10, set, grade, help = i % 4 == 0, playedAt = playedAt)
        }
    }

    /** Grade value recomputed by hand from the documented formula, independently of `GradeCalculator`. */
    fun expectedValue(chronological: List<QuizOutcome>): Double {
        val window = chronological.takeLast(20)
        return window.sumOf { q ->
            val difficulty = if (q.characterSet == CharacterSet.KANJI) 1.0 + q.grade / 2.0 else 1.0
            val penalty = if (q.help) 0.8 else 1.0
            q.score.toDouble() / q.total * difficulty * penalty
        } / window.size
    }

    /** Rank level 0..14 (0 = 10 kyū, 9 = 1 kyū, 10 = 1 dan) for a grade value on the 0..5 scale. */
    fun expectedLevel(value: Double): Int = min(14, floor(value / 5.0 * 15.0).toInt())

    /** Rank label as the app writes it ("7 kyū", "2 dan"), read from the app's own string resources. */
    fun rankLabel(level: Int): String {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        return if (level < 10) {
            resources.getString(R.string.rank_kyu, 10 - level)
        } else {
            resources.getString(R.string.rank_dan, level - 9)
        }
    }

    suspend fun insertWitness(): Long =
        repository.insert(QuizConfig(CharacterSet.HIRAGANA), score = 3, total = WITNESS_TOTAL, playedAt = 1L)

    /** Plain SQL on the app's database file, so counting and deleting do not depend on the code under test. */
    private fun <T> rawDb(block: (SQLiteDatabase) -> T): T {
        val file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(UnolingoDatabase.NAME)
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            return block(db)
        } finally {
            db.close()
        }
    }

    fun count(where: String): Int = rawDb { db ->
        db.rawQuery("SELECT COUNT(*) FROM quiz_result WHERE $where", null).use { it.moveToFirst(); it.getInt(0) }
    }

    fun countFake(): Int = count("isFake = 1")

    fun countReal(): Int = count("isFake = 0")

    fun witnessExists(id: Long): Boolean = count("id = $id AND isFake = 0") == 1

    /** Removes every witness (the current one and any left by a crashed run); never touches other real results. */
    fun deleteWitnesses() {
        rawDb { db -> db.execSQL("DELETE FROM quiz_result WHERE isFake = 0 AND total = $WITNESS_TOTAL") }
    }
}
