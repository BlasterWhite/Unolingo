package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuestionType
import bzh.enssat.unolingo.domain.model.QuizMode
import kotlin.random.Random

class QuestionGenerator(private val random: Random = Random.Default) {

    fun generate(characters: List<JapaneseCharacter>, mode: QuizMode): List<Question> =
        generate(characters, mode.questionCount, mode.questionTypes)

    /**
     * Builds [count] questions on distinct characters (fewer if [characters] is smaller), with [types] evenly
     * spread.
     */
    fun generate(characters: List<JapaneseCharacter>, count: Int, types: List<QuestionType>): List<Question> {
        val targets = characters.shuffled(random).take(count)
        val questionTypes = List(targets.size) { types[it % types.size] }.shuffled(random)
        return targets.zip(questionTypes) { character, type -> createQuestion(character, type, characters) }
    }

    private fun createQuestion(
        character: JapaneseCharacter,
        type: QuestionType,
        pool: List<JapaneseCharacter>,
    ): Question = when (type) {
        QuestionType.CHARACTER_TO_ROMAJI -> Question.CharacterToRomaji(character)
        QuestionType.CHARACTER_TO_ROMAJI_CHOICE -> {
            val distractors = DistractorPicker.pickReadings(character, pool, CHOICE_COUNT - 1, random)
            Question.CharacterToRomajiChoice(character, (distractors + character.readings.random(random)).shuffled(random))
        }
        QuestionType.ROMAJI_TO_CHARACTER -> {
            val prompt = character.readings.random(random)
            val distractors = DistractorPicker.pickCharacters(character, prompt, pool, CHOICE_COUNT - 1, random)
            Question.RomajiToCharacter(character, prompt, (distractors + character).shuffled(random))
        }
        QuestionType.SOUND_TO_CHARACTER -> {
            val prompt = character.readings.random(random)
            val distractors =
                DistractorPicker.pickCharacters(character, prompt, pool, CHOICE_COUNT - 1, random, bySound = true)
            Question.SoundToCharacter(character, prompt, (distractors + character).shuffled(random))
        }
        QuestionType.CHARACTER_TO_SOUND -> {
            val distractors = DistractorPicker.pickReadings(character, pool, CHOICE_COUNT - 1, random, bySound = true)
            Question.CharacterToSound(character, (distractors + character.readings.random(random)).shuffled(random))
        }
    }

    companion object {
        const val CHOICE_COUNT = 4
    }
}
