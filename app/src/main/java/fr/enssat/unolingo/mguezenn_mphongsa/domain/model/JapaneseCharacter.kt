package fr.enssat.unolingo.mguezenn_mphongsa.domain.model

import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaToRomaji
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.AnswerChecker

/** Hiragana readings of a kanji, as kept after cleaning: on'yomi and kun'yomi apart. */
data class KanaReadings(
    val on: List<String> = emptyList(),
    val kun: List<String> = emptyList(),
) {
    val all: List<String>
        get() = on + kun
}

data class JapaneseCharacter(
    val symbol: String,
    /** Hepburn readings that can be shown to the user. Kana have one; kanji can have several (on'yomi and kun'yomi). */
    val readings: List<String>,
    /** Other accepted spellings (Kunrei-shiki, IME input such as "nn", kun'yomi with okurigana), never displayed. */
    val alternatives: List<String> = emptyList(),
    /** School grade of a kanji (1..6, 8 = other jōyō kanji), null for kana. */
    val grade: Int? = null,
    /** English meanings of a kanji (from the TP data), empty for kana. */
    val meaningsEn: List<String> = emptyList(),
    /** Hiragana readings of a kanji, on and kun apart; empty for kana (the symbol itself is the reading). */
    val kanaReadings: KanaReadings = KanaReadings(),
    /** Symbols that are easy to confuse with this one, used to pick distractors. */
    val similar: Set<String> = emptySet(),
) {
    init {
        require(readings.isNotEmpty()) { "$symbol has no reading" }
    }

    /** Short English meaning for display (first three of [meaningsEn]), null for kana. Kept for `ui/learn`. */
    val meaning: String?
        get() = meaningsEn.take(3).joinToString(", ").ifEmpty { null }

    val acceptedAnswers: List<String>
        get() = readings + alternatives

    private val hiraganaByReading: Map<String, String> by lazy {
        buildMap {
            kanaReadings.all.forEach { kana ->
                KanaToRomaji.toRomaji(kana)?.let { putIfAbsent(AnswerChecker.normalizeRomaji(it), kana) }
            }
        }
    }

    /**
     * What text-to-speech should say for [reading] (one of [readings]): the hiragana reading taken from the data
     * file, or the symbol written in hiragana for a kana. Null if no hiragana reading matches.
     */
    fun hiraganaFor(reading: String): String? =
        if (kanaReadings.all.isEmpty()) KanaToRomaji.katakanaToHiragana(symbol)
        else hiraganaByReading[AnswerChecker.normalizeRomaji(reading)]
}
