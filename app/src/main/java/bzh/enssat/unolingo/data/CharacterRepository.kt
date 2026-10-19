package bzh.enssat.unolingo.data

import android.content.res.AssetManager
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface CharacterRepository {
    suspend fun getCharacters(set: CharacterSet): List<JapaneseCharacter>
}

class AssetCharacterRepository(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CharacterRepository {
    private val cache = ConcurrentHashMap<CharacterSet, List<JapaneseCharacter>>()

    override suspend fun getCharacters(set: CharacterSet): List<JapaneseCharacter> =
        cache[set] ?: withContext(ioDispatcher) {
            assets.open(assetPath(set)).bufferedReader().use { CharacterDataParser.parse(it.readText()) }
        }.also { cache[set] = it }

    companion object {
        fun assetPath(set: CharacterSet): String = "characters/${set.name.lowercase()}.json"
    }
}
