package fr.enssat.unolingo.mguezenn_mphongsa.data

import android.content.Context
import androidx.annotation.RawRes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Reads a raw resource (`res/raw`) as text, as described in TP1. */
fun loadJsonFromRaw(context: Context, @RawRes id: Int): String =
    context.resources.openRawResource(id).bufferedReader().use { it.readText() }

/** Same `Json` for every TP file: the kanji file has many `wk_*` fields the app does not use. */
internal val tpJson = Json { ignoreUnknownKeys = true }

// ---- Raw format of `kana_data.json`: script → section → line → entries ----

/** `{"hiragana": {"hiragana_alphabet": {"voyelles": [{"char": "あ", "romaji": "a"}, …]}, …}, "katakana": {…}}` */
@Serializable
data class RawKanaFile(
    val hiragana: Map<String, Map<String, List<RawKanaEntry>>>,
    val katakana: Map<String, Map<String, List<RawKanaEntry>>>,
)

@Serializable
data class RawKanaEntry(val char: String, val romaji: String)

// ---- Raw format of `kanji_jouyou.json`: kanji → details (kanji-data by davidluzgouveia) ----

@Serializable
data class RawKanji(
    val strokes: Int? = null,
    val grade: Int? = null,
    val freq: Int? = null,
    val meanings: List<String> = emptyList(),
    val readings_on: List<String> = emptyList(),
    val readings_kun: List<String> = emptyList(),
)

fun parseRawKana(text: String): RawKanaFile = tpJson.decodeFromString(text)

fun parseRawKanji(text: String): Map<String, RawKanji> = tpJson.decodeFromString(text)
