package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig

/**
 * Picks the characters a quiz draws from. The ~2 100 kanji are far too many for a 10-question series, so kanji are
 * restricted to the grades up to the chosen one (grade ≤ g); questions and distractors then both come from this
 * subset. Kana are never filtered.
 */
object QuizPool {
    fun select(config: QuizConfig, characters: List<JapaneseCharacter>): List<JapaneseCharacter> =
        select(config.characterSet, characters, config.grade)

    fun select(
        set: CharacterSet,
        characters: List<JapaneseCharacter>,
        grade: Int = QuizConfig.DEFAULT_KANJI_GRADE,
    ): List<JapaneseCharacter> {
        if (set != CharacterSet.KANJI) return characters
        val filtered = characters.filter { it.grade?.let { g -> g <= grade } == true }
        // A grade matching nothing (or too little data) must not leave the quiz empty.
        return filtered.ifEmpty { characters }
    }
}
