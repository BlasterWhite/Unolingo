package fr.enssat.unolingo.mguezenn_mphongsa.data

import android.content.Context
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

interface CharacterRepository {
    suspend fun getCharacters(set: CharacterSet): List<JapaneseCharacter>
}

/**
 * Reads the TP files (`R.raw.kana_data`, `R.raw.kanji_jouyou`) and the house extras once, on [ioDispatcher], and keeps
 * the mapped characters in memory.
 */
class RawCharacterRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CharacterRepository {
    private val lock = Mutex()
    @Volatile
    private var cache: Map<CharacterSet, List<JapaneseCharacter>>? = null

    override suspend fun getCharacters(set: CharacterSet): List<JapaneseCharacter> {
        cache?.let { return it.getValue(set) }
        return lock.withLock {
            cache ?: withContext(ioDispatcher) { loadAll() }.also { cache = it }
        }.getValue(set)
    }

    private fun loadAll(): Map<CharacterSet, List<JapaneseCharacter>> {
        try {
            val extras = context.assets.open(CharacterExtras.ASSET_PATH).bufferedReader()
                .use { CharacterExtras.parse(it.readText()) }
            val kana = parseRawKana(loadJsonFromRaw(context, R.raw.kana_data))
            val kanji = parseRawKanji(loadJsonFromRaw(context, R.raw.kanji_jouyou))
            return mapOf(
                CharacterSet.HIRAGANA to CharacterDataParser.mapHiragana(kana, extras),
                CharacterSet.KATAKANA to CharacterDataParser.mapKatakana(kana, extras),
                CharacterSet.KANJI to CharacterDataParser.mapKanji(kanji, extras.kanji),
            )
        } catch (e: SerializationException) {
            // QuizViewModel reports malformed data like other invalid entries.
            throw IllegalArgumentException("Malformed character data", e)
        }
    }
}
