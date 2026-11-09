package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaToRomaji
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionType

/**
 * Help text of a question (TP2), language-neutral (English meaning, kana, romaji) so it needs no translation:
 *
 * - kanji, meaning question: the readings in hiragana with their romaji ("こう (kou) · ひかり (hikari)");
 * - kanji, reading question: the meaning, plus the romaji when the prompt is a hiragana reading (one line each);
 *   the choices of "character → reading" are not romanized, which would give the answer away;
 * - kana: the same character in the other syllabary (あ ↔ ア).
 */
internal object QuestionHelp {
    private const val MAX_READINGS = 4
    private const val HIRAGANA_FIRST = 'ぁ'
    private const val HIRAGANA_LAST = 'ゖ'
    private const val KATAKANA_FIRST = 'ァ'
    private const val KATAKANA_LAST = 'ヶ'
    private const val OFFSET = 0x60

    /** A kanji has an English meaning, a school grade or hiragana readings; a kana has none of them. */
    fun isKanji(character: JapaneseCharacter): Boolean =
        character.grade != null || character.meaning != null || character.kanaReadings.all.isNotEmpty()

    /** [prompt] is the reading shown to the user for a "reading → character" question, if any. */
    fun create(character: JapaneseCharacter, type: QuestionType, prompt: String? = null): String? {
        if (!isKanji(character)) return otherSyllabary(character.symbol)
        return when (type) {
            QuestionType.CHARACTER_TO_MEANING, QuestionType.MEANING_TO_CHARACTER -> readingsLine(character)
            else -> listOfNotNull(
                character.meaning,
                prompt?.takeIf { it.any(AnswerChecker::isKana) }?.let { KanaToRomaji.toRomaji(it) },
            ).joinToString("\n").ifEmpty { null }
        }
    }

    private fun readingsLine(character: JapaneseCharacter): String? =
        character.kanaReadings.all.take(MAX_READINGS)
            .joinToString(" · ") { kana -> KanaToRomaji.toRomaji(kana)?.let { "$kana ($it)" } ?: kana }
            .ifEmpty { null }

    /** あ ↔ ア, character by character; null when nothing changes (not a kana). */
    fun otherSyllabary(symbol: String): String? {
        val converted = symbol.map {
            when (it) {
                in HIRAGANA_FIRST..HIRAGANA_LAST -> it + OFFSET
                in KATAKANA_FIRST..KATAKANA_LAST -> it - OFFSET
                else -> it
            }
        }.joinToString("")
        return converted.takeIf { it != symbol }
    }
}
