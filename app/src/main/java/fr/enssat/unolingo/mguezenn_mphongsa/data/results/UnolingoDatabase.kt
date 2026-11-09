package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import androidx.room.Database
import androidx.room.RoomDatabase

/** Local database. Version 1; the exported schema is committed in app/schemas/. */
@Database(
    entities = [QuizResultEntity::class, QuestionResultEntity::class],
    version = 1,
)
abstract class UnolingoDatabase : RoomDatabase() {
    abstract fun quizResultDao(): QuizResultDao

    companion object {
        const val NAME = "unolingo.db"
    }
}
