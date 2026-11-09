package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizResultDao {
    @Insert
    suspend fun insertQuiz(quiz: QuizResultEntity): Long

    @Insert
    suspend fun insertQuestions(questions: List<QuestionResultEntity>)

    /** Inserts a quiz and its questions atomically; returns the quiz id. */
    @Transaction
    suspend fun insert(quiz: QuizResultEntity, questions: List<QuestionResultEntity> = emptyList()): Long {
        val id = insertQuiz(quiz)
        if (questions.isNotEmpty()) insertQuestions(questions.map { it.copy(quizId = id) })
        return id
    }

    /** Every result, oldest first (the order the grade computation expects). Re-emits on each change. */
    @Query("SELECT * FROM quiz_result ORDER BY playedAt ASC, id ASC")
    fun observeAll(): Flow<List<QuizResultEntity>>

    /** The [limit] most recent results, newest first. */
    @Query("SELECT * FROM quiz_result ORDER BY playedAt DESC, id DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<QuizResultEntity>

    @Query("SELECT * FROM question_result WHERE quizId = :quizId")
    suspend fun questionsOf(quizId: Long): List<QuestionResultEntity>

    /** Deletes only test rows (isFake = 1); their question results go with them (cascade). */
    @Query("DELETE FROM quiz_result WHERE isFake = 1")
    suspend fun deleteFakeResults()
}
