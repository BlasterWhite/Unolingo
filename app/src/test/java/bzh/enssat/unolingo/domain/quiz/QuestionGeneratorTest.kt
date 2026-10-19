package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuestionType
import bzh.enssat.unolingo.domain.model.QuizMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionGeneratorTest {
    private val kana = listOf(
        "a", "i", "u", "e", "o", "ka", "ki", "ku", "ke", "ko", "sa", "shi", "su", "se", "so",
    ).mapIndexed { index, reading -> JapaneseCharacter(symbol = "k$index", readings = listOf(reading)) }

    @Test
    fun displayMode_generatesTenQuestionsOnDistinctCharacters() {
        val questions = QuestionGenerator(Random(1)).generate(kana, QuizMode.DISPLAY)

        assertEquals(10, questions.size)
        assertEquals(10, questions.map { it.character.symbol }.distinct().size)
    }

    @Test
    fun displayMode_splitsQuestionTypesEvenly() {
        val questions = QuestionGenerator(Random(2)).generate(kana, QuizMode.DISPLAY)

        val countByType = questions.groupingBy { it.type }.eachCount()
        assertEquals(mapOf(QuestionType.CHARACTER_TO_ROMAJI to 5, QuestionType.ROMAJI_TO_CHARACTER to 5), countByType)
    }

    @Test
    fun fewerCharactersThanQuestions_usesEachCharacterOnce() {
        val questions = QuestionGenerator(Random(3)).generate(kana.take(4), QuizMode.DISPLAY)

        assertEquals(4, questions.size)
        assertEquals(4, questions.map { it.character }.distinct().size)
    }

    @Test
    fun choiceQuestions_haveFourDistinctChoicesIncludingTheAnswer() {
        repeat(20) { seed ->
            QuestionGenerator(Random(seed)).generate(kana, QuizMode.DISPLAY)
                .filterIsInstance<Question.RomajiToCharacter>()
                .forEach { question ->
                    assertEquals(QuestionGenerator.CHOICE_COUNT, question.choices.distinct().size)
                    assertTrue(question.character in question.choices)
                    assertTrue(question.prompt in question.character.readings)
                }
        }
    }

    @Test
    fun choiceQuestions_neverOfferAnotherCharacterWithTheSameReading() {
        // じ and ぢ both read "ji"; 日 and 火 both read "ka".
        val characters = listOf(
            JapaneseCharacter("じ", listOf("ji")),
            JapaneseCharacter("ぢ", listOf("ji"), alternatives = listOf("di")),
            JapaneseCharacter("日", listOf("nichi", "ka")),
            JapaneseCharacter("火", listOf("ka", "hi")),
            JapaneseCharacter("を", listOf("wo"), alternatives = listOf("o")),
            JapaneseCharacter("お", listOf("o")),
            JapaneseCharacter("か", listOf("ka")),
            JapaneseCharacter("た", listOf("ta")),
            JapaneseCharacter("な", listOf("na")),
        )
        repeat(50) { seed ->
            QuestionGenerator(Random(seed)).generate(characters, QuizMode.DISPLAY)
                .filterIsInstance<Question.RomajiToCharacter>()
                .forEach { question ->
                    val matching = question.choices.filter { choice ->
                        choice.acceptedAnswers.any { AnswerChecker.normalizeRomaji(it) == question.prompt }
                    }
                    assertEquals("prompt ${question.prompt}", listOf(question.character), matching)
                }
        }
    }

    @Test
    fun choiceQuestions_preferSimilarCharacters() {
        val nu = JapaneseCharacter("ぬ", listOf("nu"), similar = setOf("め"))
        val me = JapaneseCharacter("め", listOf("me"), similar = setOf("ぬ"))
        val others = kana
        repeat(20) { seed ->
            val distractors = DistractorPicker.pickCharacters(nu, "nu", others + me, count = 3, random = Random(seed))
            assertTrue(me in distractors)
        }
    }

    @Test
    fun readingChoices_haveFourDistinctLabelsAndExactlyOneRightAnswer() {
        // 日 and 火 share "ka" and "hi": 火 must never be offered when 日 is asked.
        val kanji = listOf(
            JapaneseCharacter("日", listOf("nichi", "jitsu", "hi", "ka")),
            JapaneseCharacter("火", listOf("ka", "hi")),
            JapaneseCharacter("月", listOf("getsu", "gatsu", "tsuki")),
            JapaneseCharacter("水", listOf("sui", "mizu")),
            JapaneseCharacter("木", listOf("moku", "boku", "ki")),
            JapaneseCharacter("金", listOf("kin", "kon", "kane")),
        )
        val types = listOf(QuestionType.CHARACTER_TO_ROMAJI_CHOICE)
        repeat(50) { seed ->
            QuestionGenerator(Random(seed)).generate(kanji, count = kanji.size, types = types)
                .map { it as Question.CharacterToRomajiChoice }
                .forEach { question ->
                    assertEquals(QuestionGenerator.CHOICE_COUNT, question.choices.distinct().size)
                    val right = question.choices.filter { AnswerChecker.acceptsReading(question.character, it) }
                    assertEquals("${question.character.symbol} ${question.choices}", 1, right.size)
                }
        }
    }

    @Test
    fun audioMode_generatesTwentyQuestionsSplitEvenly() {
        val characters = kana + ('a'..'z').map { JapaneseCharacter("x$it", listOf("x$it")) }
        val questions = QuestionGenerator(Random(5)).generate(characters, QuizMode.AUDIO)

        assertEquals(20, questions.size)
        assertEquals(
            mapOf(QuestionType.SOUND_TO_CHARACTER to 10, QuestionType.CHARACTER_TO_SOUND to 10),
            questions.groupingBy { it.type }.eachCount(),
        )
    }

    // お/を, じ/ぢ and ず/づ are pronounced the same; 日/火 share readings.
    private val homophones = listOf(
        JapaneseCharacter("お", listOf("o")),
        JapaneseCharacter("を", listOf("wo"), alternatives = listOf("o")),
        JapaneseCharacter("じ", listOf("ji"), alternatives = listOf("zi")),
        JapaneseCharacter("ぢ", listOf("ji"), alternatives = listOf("di")),
        JapaneseCharacter("ず", listOf("zu")),
        JapaneseCharacter("づ", listOf("zu"), alternatives = listOf("du")),
        JapaneseCharacter("日", listOf("nichi", "hi", "ka")),
        JapaneseCharacter("火", listOf("ka", "hi")),
        JapaneseCharacter("か", listOf("ka")),
        JapaneseCharacter("た", listOf("ta")),
        JapaneseCharacter("な", listOf("na")),
        JapaneseCharacter("ま", listOf("ma")),
    )
    private val homophoneGroups = listOf(setOf("お", "を"), setOf("じ", "ぢ"), setOf("ず", "づ"), setOf("日", "火", "か"))

    @Test
    fun soundToCharacter_neverOffersHomophones() {
        val types = listOf(QuestionType.SOUND_TO_CHARACTER)
        repeat(50) { seed ->
            QuestionGenerator(Random(seed)).generate(homophones, count = homophones.size, types = types)
                .map { it as Question.SoundToCharacter }
                .forEach { question ->
                    assertEquals(QuestionGenerator.CHOICE_COUNT, question.choices.distinct().size)
                    // Wrong choices may sound alike among themselves, but never like the right one.
                    val target = question.character.symbol
                    val group = homophoneGroups.firstOrNull { target in it }.orEmpty()
                    val symbols = question.choices.map { it.symbol }
                    assertEquals("$target $symbols", listOf(target), symbols.filter { it in group || it == target })
                }
        }
    }

    @Test
    fun characterToSound_hasExactlyOneRightReadingAndNoHomophone() {
        val types = listOf(QuestionType.CHARACTER_TO_SOUND)
        repeat(50) { seed ->
            QuestionGenerator(Random(seed)).generate(homophones, count = homophones.size, types = types)
                .map { it as Question.CharacterToSound }
                .forEach { question ->
                    assertEquals(QuestionGenerator.CHOICE_COUNT, question.choices.distinct().size)
                    val right = question.choices.filter { AnswerChecker.acceptsReading(question.character, it) }
                    assertEquals("${question.character.symbol} ${question.choices}", 1, right.size)
                    // を is said "o": a distractor "wo" for お (or "o" for を) would sound identical.
                    val wrong = question.choices - right.toSet()
                    val sameSound = homophones.filter { other ->
                        other.symbol != question.character.symbol &&
                            question.character.acceptedAnswers.any { AnswerChecker.acceptsReading(other, it) }
                    }
                    wrong.forEach { reading ->
                        assertTrue(
                            "${question.character.symbol}: '$reading' sounds like it",
                            sameSound.none { AnswerChecker.acceptsReading(it, reading) },
                        )
                    }
                }
        }
    }
}
