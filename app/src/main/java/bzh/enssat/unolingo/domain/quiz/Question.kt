package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuestionType

sealed interface Question {
    /** The character being tested. */
    val character: JapaneseCharacter
    val type: QuestionType

    /** Shows [character]; the user types one of its readings. */
    data class CharacterToRomaji(override val character: JapaneseCharacter) : Question {
        override val type: QuestionType
            get() = QuestionType.CHARACTER_TO_ROMAJI
    }

    /** Shows [character]; the user picks one of its readings among [choices] (romaji labels). */
    data class CharacterToRomajiChoice(
        override val character: JapaneseCharacter,
        val choices: List<String>,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.CHARACTER_TO_ROMAJI_CHOICE
    }

    /** Pronounces [prompt], one reading of [character]; the user picks [character] among [choices]. */
    data class SoundToCharacter(
        override val character: JapaneseCharacter,
        val prompt: String,
        val choices: List<JapaneseCharacter>,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.SOUND_TO_CHARACTER
    }

    /** Shows [character]; the user listens to [choices] (readings, pronounced on demand) and picks the right one. */
    data class CharacterToSound(
        override val character: JapaneseCharacter,
        val choices: List<String>,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.CHARACTER_TO_SOUND
    }

    /** Shows [prompt], one reading of [character]; the user picks [character] among [choices]. */
    data class RomajiToCharacter(
        override val character: JapaneseCharacter,
        val prompt: String,
        val choices: List<JapaneseCharacter>,
    ) : Question {
        override val type: QuestionType
            get() = QuestionType.ROMAJI_TO_CHARACTER
    }
}

/**
 * [answer] is the typed text for [Question.CharacterToRomaji], the chosen reading for
 * [Question.CharacterToRomajiChoice] and [Question.CharacterToSound], and the chosen symbol for
 * [Question.RomajiToCharacter] and [Question.SoundToCharacter].
 */
data class QuestionResult(val question: Question, val answer: String, val isCorrect: Boolean)
