package fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaToRomaji
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionType
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.KanaReadings
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionGeneratorTest {
    private val hiragana = QuizConfig(CharacterSet.HIRAGANA)

    private val kana = listOf(
        "a", "i", "u", "e", "o", "ka", "ki", "ku", "ke", "ko", "sa", "shi", "su", "se", "so",
    ).mapIndexed { index, reading -> JapaneseCharacter(symbol = "k$index", readings = listOf(reading)) }

    @Test
    fun displayMode_generatesTenQuestionsOnDistinctCharacters() {
        val questions = QuestionGenerator(Random(1)).generate(kana, hiragana)

        assertEquals(10, questions.size)
        assertEquals(10, questions.map { it.character.symbol }.distinct().size)
    }

    @Test
    fun displayMode_splitsQuestionTypesEvenly() {
        val questions = QuestionGenerator(Random(2)).generate(kana, hiragana)

        val countByType = questions.groupingBy { it.type }.eachCount()
        assertEquals(mapOf(QuestionType.CHARACTER_TO_ROMAJI_CHOICE to 5, QuestionType.ROMAJI_TO_CHARACTER to 5), countByType)
    }

    @Test
    fun fewerCharactersThanQuestions_usesEachCharacterOnce() {
        val questions = QuestionGenerator(Random(3)).generate(kana.take(4), hiragana)

        assertEquals(4, questions.size)
        assertEquals(4, questions.map { it.character }.distinct().size)
    }

    @Test
    fun choiceQuestions_haveFourDistinctChoicesIncludingTheAnswer() {
        repeat(20) { seed ->
            QuestionGenerator(Random(seed)).generate(kana, hiragana)
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
            QuestionGenerator(Random(seed)).generate(characters, hiragana)
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
    fun ttsQuiz_generatesTenAudioQuestionsSplitEvenly() {
        val characters = kana + ('a'..'z').map { JapaneseCharacter("x$it", listOf("x$it")) }
        val questions = QuestionGenerator(Random(5)).generate(characters, QuizConfig(CharacterSet.HIRAGANA, tts = true))

        assertEquals(10, questions.size)
        assertEquals(
            mapOf(QuestionType.SOUND_TO_CHARACTER to 5, QuestionType.CHARACTER_TO_SOUND to 5),
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

    // Kanji with several readings (some shared: 日/火 "ka"/"hi") and English meanings (some shared: 日/太陽 "sun").
    private fun kanji(symbol: String, grade: Int, kana: List<String>, vararg meanings: String) = JapaneseCharacter(
        symbol = symbol,
        readings = kana.map { KanaToRomaji.toRomaji(it)!! },
        grade = grade,
        meaningsEn = meanings.toList(),
        meaning = meanings.take(3).joinToString(", "),
        kanaReadings = KanaReadings(on = kana.take(1), kun = kana.drop(1)),
    )

    private val kanji = listOf(
        kanji("日", 1, listOf("にち", "ひ", "か"), "day", "sun"),
        kanji("火", 1, listOf("か", "ひ"), "fire"),
        kanji("月", 1, listOf("げつ", "つき"), "month", "moon"),
        kanji("水", 1, listOf("すい", "みず"), "water"),
        kanji("木", 1, listOf("もく", "き"), "tree", "wood"),
        kanji("金", 1, listOf("きん", "かね"), "gold", "money"),
        kanji("太陽", 2, listOf("たいよう"), "sun"),
        kanji("山", 1, listOf("さん", "やま"), "mountain"),
        kanji("川", 1, listOf("せん", "かわ"), "river"),
        kanji("空", 1, listOf("くう", "そら"), "sky", "empty"),
        kanji("海", 2, listOf("かい", "うみ"), "sea"),
        kanji("森", 2, listOf("しん", "もり"), "forest"),
    )

    private fun kanjiQuiz(kind: QuestionKind, tts: Boolean = false, help: Boolean = false, seed: Int) =
        QuestionGenerator(Random(seed)).generate(kanji, QuizConfig(CharacterSet.KANJI, kind, help, tts))

    @Test
    fun kanjiMix_generatesTenQuestionsAcrossTheFourTypes() {
        val questions = kanjiQuiz(QuestionKind.MIX, seed = 1)

        assertEquals(10, questions.size)
        assertEquals(
            setOf(
                QuestionType.CHARACTER_TO_ROMAJI_CHOICE, QuestionType.ROMAJI_TO_CHARACTER,
                QuestionType.CHARACTER_TO_MEANING, QuestionType.MEANING_TO_CHARACTER,
            ),
            questions.map { it.type }.toSet(),
        )
        // 10 questions over 4 types: 3, 3, 2, 2.
        assertTrue(questions.groupingBy { it.type }.eachCount().values.all { it in 2..3 })
    }

    @Test
    fun everyKanjiType_hasFourDistinctChoicesAndExactlyOneRightAnswer() {
        repeat(40) { seed ->
            QuestionKind.entries.forEach { kind ->
                kanjiQuiz(kind, seed = seed).forEach { question ->
                    val where = "${question.type} ${question.character.symbol} (seed $seed)"
                    val answers = when (question) {
                        is Question.CharacterToRomajiChoice -> question.choices
                        is Question.CharacterToMeaning -> question.choices
                        is Question.RomajiToCharacter -> question.choices.map { it.symbol }
                        is Question.MeaningToCharacter -> question.choices.map { it.symbol }
                        else -> error("unexpected $where")
                    }
                    assertEquals(where, QuestionGenerator.CHOICE_COUNT, answers.distinct().size)
                    assertEquals(where, 1, answers.count { AnswerChecker.isCorrect(question, it) })
                }
            }
        }
    }

    @Test
    fun kanjiReadings_areShownInHiragana() {
        repeat(20) { seed ->
            kanjiQuiz(QuestionKind.CHARACTER_TO_READING, seed = seed).forEach {
                (it as Question.CharacterToRomajiChoice).choices.forEach { c -> assertTrue(c, c.all(AnswerChecker::isKana)) }
            }
            kanjiQuiz(QuestionKind.READING_TO_CHARACTER, seed = seed).forEach {
                val prompt = (it as Question.RomajiToCharacter).prompt
                assertTrue(prompt, prompt.all(AnswerChecker::isKana))
            }
        }
    }

    @Test
    fun hiraganaChoice_isAcceptedLikeItsRomaji() {
        val sun = kanji.first()
        assertTrue(AnswerChecker.acceptsReading(sun, "ひ"))
        assertTrue(AnswerChecker.acceptsReading(sun, "hi"))
        assertTrue(!AnswerChecker.acceptsReading(sun, "つき"))
    }

    @Test
    fun meaningQuestions_neverOfferAMeaningThatAlsoFits() {
        // 日 and 太陽 both mean "sun": they must never be proposed together.
        repeat(100) { seed ->
            kanjiQuiz(QuestionKind.CHARACTER_TO_MEANING, seed = seed).forEach { question ->
                question as Question.CharacterToMeaning
                val target = question.character
                val wrong = question.choices - target.meaning!!
                val others = kanji.filter { it.symbol != target.symbol && it.meaning in wrong }
                others.forEach { other ->
                    assertTrue("${target.symbol} / ${other.symbol}", other.meaningsEn.none { it in target.meaningsEn })
                }
            }
            kanjiQuiz(QuestionKind.MEANING_TO_CHARACTER, seed = seed).forEach { question ->
                question as Question.MeaningToCharacter
                assertEquals(question.character.meaning, question.prompt)
                question.choices.filter { it != question.character }.forEach { other ->
                    assertTrue(other.meaningsEn.none { it in question.character.meaningsEn })
                }
            }
        }
    }

    @Test
    fun tts_turnsKanjiReadingQuestionsIntoAudioAndKeepsMeaningOnesVisual() {
        val audio = kanjiQuiz(QuestionKind.MIX, tts = true, seed = 3).map { it.type }.toSet()

        assertEquals(
            setOf(
                QuestionType.SOUND_TO_CHARACTER, QuestionType.CHARACTER_TO_SOUND,
                QuestionType.CHARACTER_TO_MEANING, QuestionType.MEANING_TO_CHARACTER,
            ),
            audio,
        )
        assertTrue(
            kanjiQuiz(QuestionKind.READING_TO_CHARACTER, tts = true, seed = 3).all { it is Question.SoundToCharacter },
        )
    }

    @Test
    fun help_isAbsentUnlessEnabled() {
        QuestionKind.entries.forEach { kind ->
            assertTrue(kanjiQuiz(kind, help = false, seed = 4).all { it.help == null })
        }
        assertTrue(QuestionGenerator(Random(4)).generate(kana, hiragana).all { it.help == null })
    }

    @Test
    fun kanjiHelp_dependsOnTheQuestionType() {
        repeat(10) { seed ->
            kanjiQuiz(QuestionKind.MIX, help = true, seed = seed).forEach { question ->
                val help = checkNotNull(question.help) { "${question.type} has no help" }
                val character = question.character
                when (question) {
                    // Meaning question: hiragana and romaji readings.
                    is Question.CharacterToMeaning, is Question.MeaningToCharacter -> {
                        assertTrue(help, character.kanaReadings.all.all { "$it (" in help })
                        assertTrue(help, character.readings.all { it in help })
                    }
                    // Reading question: the meaning, plus the romaji of a hiragana prompt.
                    is Question.RomajiToCharacter -> {
                        assertTrue(help, help.startsWith(character.meaning!!))
                        assertTrue(help, help.endsWith(KanaToRomaji.toRomaji(question.prompt)!!))
                    }
                    else -> assertEquals(character.meaning, help)
                }
            }
        }
    }

    @Test
    fun kanaHelp_isTheOtherSyllabary() {
        val a = JapaneseCharacter("あ", listOf("a"))
        val ka = JapaneseCharacter("ア", listOf("a"))
        val kya = JapaneseCharacter("きゃ", listOf("kya"))
        val pool = listOf(a, ka, kya) + kana

        val questions = QuestionGenerator(Random(1)).generate(pool, QuizConfig(CharacterSet.HIRAGANA, help = true))
        questions.filter { it.character in setOf(a, ka, kya) }.forEach {
            assertEquals(
                mapOf(a to "ア", ka to "あ", kya to "キャ")[it.character],
                it.help,
            )
        }
        assertEquals("ア", QuestionHelp.otherSyllabary("あ"))
        assertEquals("ぱ", QuestionHelp.otherSyllabary("パ"))
        assertEquals(null, QuestionHelp.otherSyllabary("a"))
    }
}
