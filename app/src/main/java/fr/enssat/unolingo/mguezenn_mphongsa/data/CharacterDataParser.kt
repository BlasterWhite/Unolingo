package fr.enssat.unolingo.mguezenn_mphongsa.data

import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaToRomaji
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.KanaReadings

/**
 * Maps the raw TP data to the domain model. Pure Kotlin; the file reading is done by the repository.
 *
 * Kanji readings are cleaned as follows:
 * - every reading: katakana → hiragana (on'yomi, and a few kun'yomi such as デシメートル for 粉);
 * - kun'yomi: `-` removed, and the part before the `.` kept (`ひと.つ` → `ひと`, `-ぐち` → `ぐち`), because a kanji
 *   shown alone is read as its stem; the full word with okurigana (`ひとつ` → "hitotsu") stays an accepted answer
 *   (`alternatives`) but is never displayed;
 * - duplicates (same romaji) removed, readings [KanaToRomaji] cannot convert (っ at the start or end: かっ, っこ…)
 *   discarded. A kanji always keeps one reading: if every stem is discarded, the full kun'yomi words are tried.
 */
object CharacterDataParser {
    /** `я` (Cyrillic) typed instead of や in the TP kana file. */
    private const val CYRILLIC_YA = 'я'

    fun mapHiragana(raw: RawKanaFile, extras: CharacterExtras = CharacterExtras()) =
        mapScript(raw.hiragana, 'や', extras.hiragana)

    fun mapKatakana(raw: RawKanaFile, extras: CharacterExtras = CharacterExtras()) =
        mapScript(raw.katakana, 'ヤ', extras.katakana)

    private fun mapScript(
        script: Map<String, Map<String, List<RawKanaEntry>>>,
        ya: Char,
        extras: Map<String, CharacterExtras.Extra>,
    ): List<JapaneseCharacter> =
        script.values.flatMap { it.values.flatten() }.map { entry ->
            val symbol = entry.char.replace(CYRILLIC_YA, ya)
            val extra = extras[symbol]
            JapaneseCharacter(
                symbol = symbol,
                readings = listOf(entry.romaji),
                alternatives = extra?.alternatives.orEmpty(),
                similar = extra?.similar.orEmpty().toSet(),
            )
        }

    fun mapKanji(raw: Map<String, RawKanji>, extras: Map<String, CharacterExtras.Extra> = emptyMap()): List<JapaneseCharacter> =
        raw.map { (symbol, kanji) -> mapKanji(symbol, kanji, extras[symbol]) }

    fun mapKanji(symbol: String, kanji: RawKanji, extra: CharacterExtras.Extra? = null): JapaneseCharacter {
        val on = kanji.readings_on.map { clean(it) }
        val kunStems = kanji.readings_kun.map { clean(it.substringBefore('.')) }
        val kunWords = kanji.readings_kun.map { clean(it.replace(".", "")) }

        var kept = convertible(on, kunStems)
        if (kept.isEmpty()) kept = convertible(on, kunWords)
        val keptRomaji = kept.map { it.romaji }.toSet()
        val fullWords = convertible(emptyList(), kunWords).map { it.romaji }.filter { it !in keptRomaji }

        return JapaneseCharacter(
            symbol = symbol,
            readings = kept.map { it.romaji },
            alternatives = (fullWords + extra?.alternatives.orEmpty()).distinct(),
            grade = kanji.grade,
            meaningsEn = kanji.meanings,
            meaning = kanji.meanings.take(3).joinToString(", ").ifEmpty { null },
            kanaReadings = KanaReadings(on = kept.filter { it.isOn }.map { it.kana }, kun = kept.filterNot { it.isOn }.map { it.kana }),
            similar = extra?.similar.orEmpty().toSet(),
        )
    }

    private class Reading(val kana: String, val romaji: String, val isOn: Boolean)

    private fun clean(reading: String) = KanaToRomaji.katakanaToHiragana(reading.replace("-", "").trim())

    /** Readings [KanaToRomaji] can convert, in order, without two readings sharing the same romaji. */
    private fun convertible(on: List<String>, kun: List<String>): List<Reading> {
        val seen = mutableSetOf<String>()
        return (on.map { it to true } + kun.map { it to false }).mapNotNull { (kana, isOn) ->
            val romaji = KanaToRomaji.toRomaji(kana) ?: return@mapNotNull null
            if (seen.add(romaji)) Reading(kana, romaji, isOn) else null
        }
    }
}
