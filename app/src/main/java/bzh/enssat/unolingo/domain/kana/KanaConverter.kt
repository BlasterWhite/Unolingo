package bzh.enssat.unolingo.domain.kana

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.quiz.AnswerChecker

/**
 * Writes romaji readings in hiragana, so that text-to-speech pronounces exactly the expected reading (a lone kanji
 * such as 日 could be read any of its ways). Built from the hiragana data rather than a hard-coded table.
 */
class KanaConverter(hiragana: List<JapaneseCharacter>) {
    // First occurrence wins: "ji" → じ (basic row) rather than ぢ, "zu" → ず rather than づ.
    private val kanaByRomaji: Map<String, String> = buildMap {
        hiragana.forEach { putIfAbsent(it.readings.first(), it.symbol) }
    }
    private val longestSyllable = kanaByRomaji.keys.maxOfOrNull { it.length } ?: 0

    /** Null if [reading] cannot be split into known syllables. */
    fun toHiragana(reading: String): String? {
        val romaji = AnswerChecker.normalizeRomaji(reading)
        if (romaji.isEmpty()) return null
        val result = StringBuilder()
        var index = 0
        while (index < romaji.length) {
            // Longest match first: "kyu" before "ky…", "ni" before "n" ("nichi" → にち, but "onna" → おんな).
            val length = (minOf(longestSyllable, romaji.length - index) downTo 1)
                .firstOrNull { romaji.substring(index, index + it) in kanaByRomaji }
                ?: return null
            result.append(kanaByRomaji.getValue(romaji.substring(index, index + length)))
            index += length
        }
        return result.toString()
    }
}
