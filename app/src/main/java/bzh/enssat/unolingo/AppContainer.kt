package bzh.enssat.unolingo

import android.content.Context
import bzh.enssat.unolingo.data.AssetCharacterRepository
import bzh.enssat.unolingo.data.CharacterRepository

/** Manual dependency injection: app-wide singletons, created once by [UnolingoApplication]. */
class AppContainer(context: Context) {
    val characterRepository: CharacterRepository = AssetCharacterRepository(context.assets)
}
