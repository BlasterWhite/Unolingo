package bzh.enssat.unolingo

import android.content.Context
import bzh.enssat.unolingo.data.AssetCharacterRepository
import bzh.enssat.unolingo.data.CharacterRepository
import bzh.enssat.unolingo.sound.AnswerSoundPlayer
import bzh.enssat.unolingo.sound.SoundPoolAnswerPlayer

/** Manual dependency injection: app-wide singletons, created once by [UnolingoApplication]. */
class AppContainer(context: Context) {
    val characterRepository: CharacterRepository = AssetCharacterRepository(context.assets)
    val answerSounds: AnswerSoundPlayer = SoundPoolAnswerPlayer(context)
}
