package fr.enssat.unolingo.mguezenn_mphongsa.domain.kana

/**
 * Converts a kana reading (hiragana or katakana, as found in the kanji data) to Hepburn romaji, with the same
 * conventions as the character data: wāpuro long vowels (こう → "kou", じゅう → "juu"), を → "wo", ぢ → "ji", づ → "zu".
 *
 * Deterministic single left-to-right pass without backtracking: longest match first (a two-character yōon such as きゃ,
 * then one character), looking only at the next character for っ, ん and ー. Pure Kotlin, no Android dependency.
 *
 * This is deliberately separate from [KanaConverter] (romaji → hiragana, greedy and ambiguous, reserved for kana).
 */
object KanaToRomaji {
    private const val KATAKANA_FIRST = 'ァ'
    private const val KATAKANA_LAST = 'ヶ'
    private const val KATAKANA_TO_HIRAGANA_OFFSET = 0x60
    private const val VU = 'ヴ'
    private const val SMALL_TSU = 'っ'
    private const val MORA_N = 'ん'
    private const val LONG_MARK = 'ー'
    private const val VOWELS = "aiueo"

    private val syllables: Map<String, String> = buildMap {
        val rows = listOf(
            "あいうえお" to listOf("a", "i", "u", "e", "o"),
            "かきくけこ" to listOf("ka", "ki", "ku", "ke", "ko"),
            "がぎぐげご" to listOf("ga", "gi", "gu", "ge", "go"),
            "さしすせそ" to listOf("sa", "shi", "su", "se", "so"),
            "ざじずぜぞ" to listOf("za", "ji", "zu", "ze", "zo"),
            "たちつてと" to listOf("ta", "chi", "tsu", "te", "to"),
            "だぢづでど" to listOf("da", "ji", "zu", "de", "do"),
            "なにぬねの" to listOf("na", "ni", "nu", "ne", "no"),
            "はひふへほ" to listOf("ha", "hi", "fu", "he", "ho"),
            "ばびぶべぼ" to listOf("ba", "bi", "bu", "be", "bo"),
            "ぱぴぷぺぽ" to listOf("pa", "pi", "pu", "pe", "po"),
            "まみむめも" to listOf("ma", "mi", "mu", "me", "mo"),
            "らりるれろ" to listOf("ra", "ri", "ru", "re", "ro"),
        )
        rows.forEach { (kana, romaji) -> kana.forEachIndexed { i, c -> put(c.toString(), romaji[i]) } }
        put("や", "ya"); put("ゆ", "yu"); put("よ", "yo")
        put("わ", "wa"); put("を", "wo")
        put("ゔ", "vu")

        // Yōon: base kana + small ゃ/ゅ/ょ.
        val yoonBases = mapOf(
            'き' to "ky", 'ぎ' to "gy", 'し' to "sh", 'じ' to "j", 'ち' to "ch", 'ぢ' to "j", 'に' to "ny",
            'ひ' to "hy", 'び' to "by", 'ぴ' to "py", 'み' to "my", 'り' to "ry",
        )
        yoonBases.forEach { (base, consonant) ->
            put("${base}ゃ", "${consonant}a"); put("${base}ゅ", "${consonant}u"); put("${base}ょ", "${consonant}o")
        }

        // Loanword combinations that appear in katakana readings.
        put("ふぁ", "fa"); put("ふぃ", "fi"); put("ふぇ", "fe"); put("ふぉ", "fo")
        put("うぃ", "wi"); put("うぇ", "we"); put("うぉ", "wo")
        put("てぃ", "ti"); put("でぃ", "di")
        put("しぇ", "she"); put("じぇ", "je"); put("ちぇ", "che")
    }
    private val longestSyllable = syllables.keys.maxOf { it.length }

    /** Katakana → hiragana by the 0x60 code point offset; every other character, ー included, is left unchanged. */
    fun katakanaToHiragana(text: String): String = buildString(text.length) {
        for (c in text) {
            append(
                when {
                    c == VU -> 'ゔ'
                    c in KATAKANA_FIRST..KATAKANA_LAST -> (c.code - KATAKANA_TO_HIRAGANA_OFFSET).toChar()
                    else -> c
                },
            )
        }
    }

    /**
     * Hepburn romaji of [reading], or null if it cannot be converted (empty, unknown character, っ at the start or end
     * or not followed by a consonant syllable, ー or ー with no vowel before it). The caller decides
     * whether to discard such a reading.
     */
    fun toRomaji(reading: String): String? {
        val kana = katakanaToHiragana(reading)
        if (kana.isEmpty()) return null
        val out = StringBuilder()
        var lastVowel: Char? = null // vowel the previous syllable ended with, for ー
        var index = 0
        while (index < kana.length) {
            val c = kana[index]
            when (c) {
                SMALL_TSU -> {
                    if (out.isEmpty()) return null // nothing before it to be a geminate of
                    val next = syllableAt(kana, index + 1) ?: return null
                    val romaji = syllables.getValue(next)
                    if (romaji[0] in VOWELS) return null
                    // っち → "tch", otherwise double the consonant ("kk", "ss", "tt"…).
                    out.append(if (romaji.startsWith("ch")) 't' else romaji[0])
                    lastVowel = null
                    index++
                }
                MORA_N -> {
                    val next = kana.getOrNull(index + 1)
                    out.append(if (next != null && next in "あいうえおやゆよ") "n'" else "n")
                    lastVowel = null
                    index++
                }
                LONG_MARK -> {
                    out.append(lastVowel ?: return null)
                    index++
                }
                else -> {
                    val syllable = syllableAt(kana, index) ?: return null
                    val romaji = syllables.getValue(syllable)
                    out.append(romaji)
                    lastVowel = romaji.last()
                    index += syllable.length
                }
            }
        }
        return out.toString()
    }

    /** Longest known syllable (two characters, then one) starting at [index], or null. */
    private fun syllableAt(kana: String, index: Int): String? {
        for (length in minOf(longestSyllable, kana.length - index) downTo 1) {
            val candidate = kana.substring(index, index + length)
            if (candidate in syllables) return candidate
        }
        return null
    }
}
