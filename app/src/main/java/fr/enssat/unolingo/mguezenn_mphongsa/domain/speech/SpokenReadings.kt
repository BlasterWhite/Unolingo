package fr.enssat.unolingo.mguezenn_mphongsa.domain.speech

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.AnswerChecker

/**
 * Finds the hiragana to pronounce for any romaji reading of a set of characters.
 *
 * A "character → sound" question offers the readings of other characters as wrong proposals, so a reading cannot be
 * looked up on the question's own character: each reading is mapped to the hiragana of the character it belongs to.
 * The first character wins when two share a reading; they are then pronounced the same anyway.
 */
class SpokenReadings(characters: List<JapaneseCharacter>) {
    private val hiraganaByReading: Map<String, String> = buildMap {
        characters.forEach { character ->
            character.readings.forEach { reading ->
                val key = AnswerChecker.normalizeRomaji(reading)
                if (key !in this) character.hiraganaFor(reading)?.let { put(key, it) }
            }
        }
    }

    /** Hiragana to speak for [reading], or null if no character of the set has it. */
    fun hiraganaFor(reading: String): String? = hiraganaByReading[AnswerChecker.normalizeRomaji(reading)]
}
