package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One answered question of a finished quiz: the asked character's symbol and whether it was answered right. */
data class QuestionAnswer(val symbol: String, val correct: Boolean)

interface ResultsRepository {
    /** Saves a finished quiz (and per-character answers); returns its id. */
    suspend fun insert(config: QuizConfig, score: Int, total: Int, playedAt: Long, answers: List<QuestionAnswer> = emptyList()): Long

    /** All outcomes, oldest first, re-emitted on each change. */
    fun observeOutcomes(): Flow<List<QuizOutcome>>

    /** The [count] latest outcomes, oldest first (ready for `GradeCalculator`). */
    suspend fun latestOutcomes(count: Int): List<QuizOutcome>

    /** For tests only: inserts results flagged as fake. */
    suspend fun insertFake(outcomes: List<QuizOutcome>)

    /** For tests only: deletes the fake results and nothing else. */
    suspend fun deleteFake()
}

class RoomResultsRepository(private val dao: QuizResultDao) : ResultsRepository {
    override suspend fun insert(config: QuizConfig, score: Int, total: Int, playedAt: Long, answers: List<QuestionAnswer>): Long =
        dao.insert(
            QuizResultEntity(
                playedAt = playedAt,
                characterSet = config.characterSet,
                questionKind = config.questionKind,
                help = config.help,
                tts = config.tts,
                grade = config.grade,
                score = score,
                total = total,
            ),
            answers.map { QuestionResultEntity(quizId = 0, symbol = it.symbol, correct = it.correct) },
        )

    override fun observeOutcomes(): Flow<List<QuizOutcome>> = dao.observeAll().map { rows -> rows.map { it.toOutcome() } }

    override suspend fun latestOutcomes(count: Int): List<QuizOutcome> =
        dao.latest(count).map { it.toOutcome() }.reversed()

    override suspend fun insertFake(outcomes: List<QuizOutcome>) {
        outcomes.forEach { dao.insert(it.toFakeEntity()) }
    }

    override suspend fun deleteFake() = dao.deleteFakeResults()
}
