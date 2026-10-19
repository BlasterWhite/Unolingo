package bzh.enssat.unolingo.domain.model

data class JapaneseCharacter(
    val symbol: String,
    /** Hepburn readings that can be shown to the user. Kana have one; kanji can have several (on'yomi and kun'yomi). */
    val readings: List<String>,
    /** Other accepted spellings (Kunrei-shiki, IME input such as "nn"), never displayed. */
    val alternatives: List<String> = emptyList(),
    /** French meaning, for kanji. */
    val meaning: String? = null,
    /** Symbols that are easy to confuse with this one, used to pick distractors. */
    val similar: Set<String> = emptySet(),
) {
    init {
        require(readings.isNotEmpty()) { "$symbol has no reading" }
    }

    val acceptedAnswers: List<String>
        get() = readings + alternatives
}
