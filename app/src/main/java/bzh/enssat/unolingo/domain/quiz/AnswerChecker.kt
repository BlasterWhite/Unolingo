package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import java.util.Locale

object AnswerChecker {
    private val LONG_VOWELS = mapOf(
        'ā' to "aa", 'ī' to "ii", 'ū' to "uu", 'ē' to "ee", 'ō' to "ou",
        'â' to "aa", 'î' to "ii", 'û' to "uu", 'ê' to "ee", 'ô' to "ou",
    )
    private val IGNORED = Regex("[\\s'’-]")

    fun isCorrect(question: Question, answer: String): Boolean = when (question) {
        is Question.CharacterToRomaji -> acceptsReading(question.character, answer)
        is Question.CharacterToRomajiChoice -> acceptsReading(question.character, answer)
        is Question.CharacterToSound -> acceptsReading(question.character, answer)
        is Question.RomajiToCharacter -> answer == question.character.symbol
        is Question.SoundToCharacter -> answer == question.character.symbol
    }

    fun acceptsReading(character: JapaneseCharacter, reading: String): Boolean {
        val input = normalizeRomaji(reading)
        return input.isNotEmpty() && character.acceptedAnswers.any { normalizeRomaji(it) == input }
    }

    /** Case-insensitive, ignores spaces/apostrophes/hyphens, and writes macrons as doubled vowels ("kō" == "kou"). */
    fun normalizeRomaji(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(IGNORED, "")
            .map { LONG_VOWELS[it] ?: it.toString() }
            .joinToString("")
}
