package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Outcome of one question of a quiz, for the statistics (weakest characters). Deleted with its quiz. */
@Entity(
    tableName = "question_result",
    foreignKeys = [
        ForeignKey(
            entity = QuizResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("quizId")],
)
data class QuestionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quizId: Long,
    /** The character that was asked (its symbol, e.g. "あ" or "日"). */
    val symbol: String,
    val correct: Boolean,
)
