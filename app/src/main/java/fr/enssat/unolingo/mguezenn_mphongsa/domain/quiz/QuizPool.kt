package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter

/**
 * Picks the characters a quiz draws from. The ~2 100 kanji are far too many for a 10-question series, so kanji are
 * restricted to a few school grades; questions and distractors then both come from this subset. Kana are never
 * filtered.
 */
object QuizPool {
    /** Kanji grades used by default: the 240 kanji of grades 1 and 2. */
    val DEFAULT_KANJI_GRADES: Set<Int> = setOf(1, 2)

    fun select(
        set: CharacterSet,
        characters: List<JapaneseCharacter>,
        kanjiGrades: Set<Int> = DEFAULT_KANJI_GRADES,
    ): List<JapaneseCharacter> {
        if (set != CharacterSet.KANJI) return characters
        val filtered = characters.filter { it.grade in kanjiGrades }
        // A grade selection matching nothing (or too little data) must not leave the quiz empty.
        return filtered.ifEmpty { characters }
    }
}
