package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionType

/** Every question is multiple choice. */
sealed interface Question {
    /** The character being tested. */
    val character: JapaneseCharacter
    val type: QuestionType

    /** Optional help text, only filled when the quiz has help enabled (see [QuestionHelp]). */
    val help: String?

    /**
     * Shows [character]; the user picks one of its readings among [choices]: romaji labels for kana, hiragana for
     * kanji (both are accepted by [AnswerChecker]).
     */
    data class CharacterToRomajiChoice(
        override val character: JapaneseCharacter,
        val choices: List<String>,
        override val help: String? = null,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.CHARACTER_TO_ROMAJI_CHOICE
    }

    /** Pronounces [prompt], one reading of [character]; the user picks [character] among [choices]. */
    data class SoundToCharacter(
        override val character: JapaneseCharacter,
        val prompt: String,
        val choices: List<JapaneseCharacter>,
        override val help: String? = null,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.SOUND_TO_CHARACTER
    }

    /** Shows [character]; the user listens to [choices] (readings, pronounced on demand) and picks the right one. */
    data class CharacterToSound(
        override val character: JapaneseCharacter,
        val choices: List<String>,
        override val help: String? = null,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.CHARACTER_TO_SOUND
    }

    /** Shows [prompt], one reading of [character] (romaji for kana, hiragana for kanji); the user picks [character]. */
    data class RomajiToCharacter(
        override val character: JapaneseCharacter,
        val prompt: String,
        val choices: List<JapaneseCharacter>,
        override val help: String? = null,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.ROMAJI_TO_CHARACTER
    }

    /** Kanji: shows [character]; the user picks its meaning among [choices] (the `meaning` text of 4 kanji). */
    data class CharacterToMeaning(
        override val character: JapaneseCharacter,
        val choices: List<String>,
        override val help: String? = null,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.CHARACTER_TO_MEANING
    }

    /** Kanji: shows [prompt], the meaning of [character]; the user picks [character] among [choices]. */
    data class MeaningToCharacter(
        override val character: JapaneseCharacter,
        val prompt: String,
        val choices: List<JapaneseCharacter>,
        override val help: String? = null,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.MEANING_TO_CHARACTER
    }
}

/**
 * [answer] is the chosen reading for [Question.CharacterToRomajiChoice] and [Question.CharacterToSound], the chosen
 * meaning for [Question.CharacterToMeaning], and the chosen symbol for the questions that propose characters.
 */
data class QuestionResult(val question: Question, val answer: String, val isCorrect: Boolean)
