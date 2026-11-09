package fr.enssat.unolingo.mguezenn_mphongsa

import android.content.Context
import androidx.room.Room
import fr.enssat.unolingo.mguezenn_mphongsa.data.RawCharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.ResultsRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.RoomResultsRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.UnolingoDatabase
import fr.enssat.unolingo.mguezenn_mphongsa.sound.AnswerSoundPlayer
import fr.enssat.unolingo.mguezenn_mphongsa.sound.SoundPoolAnswerPlayer

/** Manual dependency injection: app-wide singletons, created once by [UnolingoApplication]. */
class AppContainer(context: Context) {
    val characterRepository: CharacterRepository = RawCharacterRepository(context)
    private val database: UnolingoDatabase =
        Room.databaseBuilder(context.applicationContext, UnolingoDatabase::class.java, UnolingoDatabase.NAME).build()
    val resultsRepository: ResultsRepository = RoomResultsRepository(database.quizResultDao())
    val answerSounds: AnswerSoundPlayer = SoundPoolAnswerPlayer(context)
}
