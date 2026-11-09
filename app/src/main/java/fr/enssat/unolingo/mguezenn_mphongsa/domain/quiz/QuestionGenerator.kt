package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionType
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import kotlin.random.Random

class QuestionGenerator(private val random: Random = Random.Default) {

    /** The [QuizConfig.QUESTION_COUNT] questions of a quiz, drawn from [characters] (see [QuizPool]). */
    fun generate(characters: List<JapaneseCharacter>, config: QuizConfig): List<Question> =
        generate(characters, QuizConfig.QUESTION_COUNT, config.questionTypes, config.help)

    /**
     * Builds [count] questions on distinct characters (fewer if [characters] is smaller), with [types] evenly
     * spread. [help] fills [Question.help].
     */
    fun generate(
        characters: List<JapaneseCharacter>,
        count: Int,
        types: List<QuestionType>,
        help: Boolean = false,
    ): List<Question> {
        val targets = characters.shuffled(random).take(count)
        val questionTypes = List(targets.size) { types[it % types.size] }.shuffled(random)
        return targets.zip(questionTypes) { character, type -> createQuestion(character, type, characters, help) }
    }

    private fun createQuestion(
        character: JapaneseCharacter,
        requested: QuestionType,
        pool: List<JapaneseCharacter>,
        help: Boolean,
    ): Question {
        val type = effectiveType(character, requested)
        fun helpFor(prompt: String? = null) = if (help) QuestionHelp.create(character, type, prompt) else null
        // Kanji readings are shown as hiragana, kana readings as romaji.
        val kanji = QuestionHelp.isKanji(character)
        return when (type) {
            QuestionType.CHARACTER_TO_ROMAJI, QuestionType.CHARACTER_TO_ROMAJI_CHOICE -> {
                val readingsOf = { c: JapaneseCharacter -> if (kanji) displayReadings(c) else c.readings }
                val distractors =
                    DistractorPicker.pickReadings(character, pool, CHOICE_COUNT - 1, random, readingsOf = readingsOf)
                val answer = readingsOf(character).random(random)
                Question.CharacterToRomajiChoice(character, (distractors + answer).shuffled(random), helpFor())
            }
            QuestionType.ROMAJI_TO_CHARACTER -> {
                val prompt = (if (kanji) displayReadings(character) else character.readings).random(random)
                val distractors = DistractorPicker.pickCharacters(character, prompt, pool, CHOICE_COUNT - 1, random)
                Question.RomajiToCharacter(character, prompt, (distractors + character).shuffled(random), helpFor(prompt))
            }
            QuestionType.SOUND_TO_CHARACTER -> {
                val prompt = character.readings.random(random)
                val distractors =
                    DistractorPicker.pickCharacters(character, prompt, pool, CHOICE_COUNT - 1, random, bySound = true)
                Question.SoundToCharacter(character, prompt, (distractors + character).shuffled(random), helpFor())
            }
            QuestionType.CHARACTER_TO_SOUND -> {
                val distractors =
                    DistractorPicker.pickReadings(character, pool, CHOICE_COUNT - 1, random, bySound = true)
                val answer = character.readings.random(random)
                Question.CharacterToSound(character, (distractors + answer).shuffled(random), helpFor())
            }
            QuestionType.CHARACTER_TO_MEANING -> {
                val distractors = DistractorPicker.pickByMeaning(character, pool, CHOICE_COUNT - 1, random)
                val choices = distractors.map { checkNotNull(it.meaning) } + checkNotNull(character.meaning)
                Question.CharacterToMeaning(character, choices.shuffled(random), helpFor())
            }
            QuestionType.MEANING_TO_CHARACTER -> {
                val distractors = DistractorPicker.pickByMeaning(character, pool, CHOICE_COUNT - 1, random)
                Question.MeaningToCharacter(
                    character, checkNotNull(character.meaning), (distractors + character).shuffled(random), helpFor(),
                )
            }
        }
    }

    /** A character with no meaning cannot be asked a meaning question: it gets the matching reading question. */
    private fun effectiveType(character: JapaneseCharacter, type: QuestionType): QuestionType =
        if (character.meaning != null) {
            type
        } else {
            when (type) {
                QuestionType.CHARACTER_TO_MEANING -> QuestionType.CHARACTER_TO_ROMAJI_CHOICE
                QuestionType.MEANING_TO_CHARACTER -> QuestionType.ROMAJI_TO_CHARACTER
                else -> type
            }
        }

    /** Hiragana readings of a kanji (what is displayed), or the romaji if the data has none. */
    private fun displayReadings(character: JapaneseCharacter): List<String> =
        character.kanaReadings.all.ifEmpty { character.readings }

    companion object {
        const val CHOICE_COUNT = 4
    }
}
