package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind

/**
 * One finished quiz. Enums are stored by name (Room's built-in enum converter), so renaming a constant is a schema
 * migration.
 *
 * [isFake] marks rows inserted by tests: [QuizResultDao.deleteFakeResults] removes only those, never real results.
 */
@Entity(tableName = "quiz_result")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch milliseconds. */
    val playedAt: Long,
    val characterSet: CharacterSet,
    val questionKind: QuestionKind,
    val help: Boolean,
    val tts: Boolean,
    val grade: Int,
    val score: Int,
    val total: Int,
    val isFake: Boolean = false,
)
