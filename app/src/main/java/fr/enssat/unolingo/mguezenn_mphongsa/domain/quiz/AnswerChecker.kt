package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaToRomaji
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import java.util.Locale

object AnswerChecker {
    private val LONG_VOWELS = mapOf(
        'ā' to "aa", 'ī' to "ii", 'ū' to "uu", 'ē' to "ee", 'ō' to "ou",
        'â' to "aa", 'î' to "ii", 'û' to "uu", 'ê' to "ee", 'ô' to "ou",
    )
    private val IGNORED = Regex("[\\s'’-]")

    fun isCorrect(question: Question, answer: String): Boolean = when (question) {
        is Question.CharacterToRomajiChoice -> acceptsReading(question.character, answer)
        is Question.CharacterToSound -> acceptsReading(question.character, answer)
        is Question.RomajiToCharacter -> answer == question.character.symbol
        is Question.SoundToCharacter -> answer == question.character.symbol
        is Question.MeaningToCharacter -> answer == question.character.symbol
        is Question.CharacterToMeaning -> answer == question.character.meaning
    }

    fun acceptsReading(character: JapaneseCharacter, reading: String): Boolean {
        val input = normalizeRomaji(reading)
        return input.isNotEmpty() && character.acceptedAnswers.any { normalizeRomaji(it) == input }
    }

    /**
     * Case-insensitive, ignores spaces/apostrophes/hyphens, and writes macrons as doubled vowels ("kō" == "kou").
     * A reading written in kana is first converted to romaji, so "こう" and "kou" are the same answer.
     */
    fun normalizeRomaji(text: String): String =
        (if (text.any(::isKana)) KanaToRomaji.toRomaji(text.trim()) ?: text else text)
            .lowercase(Locale.ROOT)
            .replace(IGNORED, "")
            .map { LONG_VOWELS[it] ?: it.toString() }
            .joinToString("")

    /** True for hiragana and katakana (not for the kanji or romaji). */
    fun isKana(c: Char): Boolean = c in '\u3041'..'\u30FF'
}
