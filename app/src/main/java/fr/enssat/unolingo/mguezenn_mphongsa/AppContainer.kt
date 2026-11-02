package fr.enssat.unolingo.mguezenn_mphongsa

import android.content.Context
import fr.enssat.unolingo.mguezenn_mphongsa.data.RawCharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.sound.AnswerSoundPlayer
import fr.enssat.unolingo.mguezenn_mphongsa.sound.SoundPoolAnswerPlayer

/** Manual dependency injection: app-wide singletons, created once by [UnolingoApplication]. */
class AppContainer(context: Context) {
    val characterRepository: CharacterRepository = RawCharacterRepository(context)
    val answerSounds: AnswerSoundPlayer = SoundPoolAnswerPlayer(context)
}
