package fr.enssat.unolingo.mguezenn_mphongsa.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.Question
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.QuestionResult
import fr.enssat.unolingo.mguezenn_mphongsa.ui.battle.BattleScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.ChoiceTestTag
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.Hanamaru
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.HomeScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.learn.LearnScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation.UnolingoNavHost
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizActions
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizContent
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizUiState
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizViewModel
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup.QuizSetupScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup.QuizSetupSettings
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.onNodeWithTag
import fr.enssat.unolingo.mguezenn_mphongsa.UnolingoApplication
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.GradeCardTag
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.HomeContent
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.ProgressChartTag
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.StatsContent
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.StatsUiState
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.gradeSummary
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.statsUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Renders the real screens (view models, character data) on the JVM and saves PNGs to `app/build/screenshots/`,
 * to review the art direction (docs/ui) without a device. Also checks that each screen composes and reacts to taps.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(content: @Composable () -> Unit) = compose.setContent { UnolingoTheme { content() } }

    private fun capture(name: String) {
        compose.waitForIdle()
        // captureToImage() waits for a hardware redraw that Robolectric never produces: draw the window ourselves.
        val bitmap = compose.runOnUiThread {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val directory = File("build/screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 10_000) =
        compose.waitUntil(timeoutMillis) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    /** Answers the current question: types a reading if there is a text field, else picks the first choice. */
    private fun answerCurrentQuestion() {
        compose.onAllNodesWithTag(ChoiceTestTag).onFirst().performClick()
    }

    @Test
    fun home() {
        show { HomeScreen(onOpenQuizSetup = {}, onLearn = {}, onOpenStats = {}, onOpenBattle = {}) }
        waitForText("104 car.")
        capture("home")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeDark() {
        show { HomeScreen(onOpenQuizSetup = {}, onLearn = {}, onOpenStats = {}, onOpenBattle = {}) }
        waitForText("104 car.")
        capture("home-dark")
    }

    private fun fakeOutcomes(count: Int) = List(count) { i ->
        val kanji = i % 3 == 2
        QuizOutcome(
            score = (5 + (i * 7) % 6).coerceAtMost(10),
            total = 10,
            characterSet = if (kanji) CharacterSet.KANJI else if (i % 2 == 0) CharacterSet.HIRAGANA else CharacterSet.KATAKANA,
            grade = if (kanji) 1 + (i / 3) % 4 else 1,
            help = i % 5 == 0,
            playedAt = 1_760_000_000_000L + i * 86_400_000L,
        )
    }

    @Test
    fun home_withGrade() {
        show { HomeContent(mapOf(CharacterSet.HIRAGANA to 104), gradeSummary(fakeOutcomes(25)), {}, {}, {}, {}, {}) }
        compose.onNodeWithTag(GradeCardTag).assertExists()
        compose.onAllNodesWithText("Votre grade").onFirst().assertExists()
        capture("home-grade")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeDark_withGrade() {
        show { HomeContent(emptyMap(), gradeSummary(fakeOutcomes(25)), {}, {}, {}, {}, {}) }
        capture("home-grade-dark")
    }

    /** Through the real repository: the card appears and updates live, and a click opens the progress page. */
    @Test
    fun home_gradeCardLiveFromRepository_opensStats() {
        val repository = (compose.activity.application as UnolingoApplication).container.resultsRepository
        try {
            show { UnolingoNavHost() }
            waitForText("Votre grade")
            runBlocking { repository.insertFake(fakeOutcomes(3)) }
            waitForText("Fenêtre de calcul : 3 / 20 quiz")
            compose.onNodeWithTag(GradeCardTag).performClick()
            waitForText("Progression du grade")
            compose.onNodeWithTag(ProgressChartTag).assertExists()
        } finally {
            runBlocking { repository.deleteFake() }
        }
    }

    @Test
    fun stats_empty() {
        var started = false
        show { StatsContent(StatsUiState.Empty, onBack = {}, onStartQuiz = { started = true }) }
        waitForText("Faites votre premier quiz")
        compose.onNodeWithTag(ProgressChartTag).assertDoesNotExist()
        capture("stats-empty")
        compose.onAllNodesWithText("Commencer un quiz").onFirst().performClick()
        assertTrue(started)
    }

    @Test
    fun stats_withManyQuizzes() {
        show { StatsContent(statsUiState(fakeOutcomes(25)), onBack = {}, onStartQuiz = {}) }
        waitForText("Progression du grade")
        compose.onNodeWithTag(ProgressChartTag).assertContentDescriptionContains("25 points", substring = true)
        compose.onNodeWithTag(ProgressChartTag).assertContentDescriptionContains("rang actuel", substring = true)
        capture("stats")
    }

    @Test
    @Config(qualifiers = "+night")
    fun statsDark_withManyQuizzes() {
        show { StatsContent(statsUiState(fakeOutcomes(25)), onBack = {}, onStartQuiz = {}) }
        waitForText("Progression du grade")
        capture("stats-dark")
    }

    @Test
    fun stats_singleQuiz() {
        show { StatsContent(statsUiState(fakeOutcomes(1)), onBack = {}, onStartQuiz = {}) }
        waitForText("Progression du grade")
        compose.onNodeWithTag(ProgressChartTag).assertContentDescriptionContains("1 point,", substring = true)
        capture("stats-one-quiz")
    }

    @Test
    fun quizSetup_kanaAndKanji() {
        var settings by mutableStateOf(QuizSetupSettings())
        var started: QuizConfig? = null
        show { QuizSetupScreen(settings, { settings = it }, { started = it }, onBack = {}) }
        waitForText("Réglages du quiz")
        capture("quiz-setup-kana")
        compose.onAllNodesWithText("Kanji").onFirst().performClick()
        waitForText("Kanji jusqu'au grade 2 · 240 kanji")
        compose.onAllNodesWithText("Sens → caractère").onFirst().performClick()
        compose.onAllNodesWithText("Aide").onFirst().performClick()
        capture("quiz-setup-kanji")
        compose.onAllNodesWithText("Commencer").onFirst().performClick()
        assertEquals(QuizConfig(CharacterSet.KANJI, QuestionKind.MEANING_TO_CHARACTER, help = true, grade = 2), started)
    }

    /** Through the real nav host: the home's set is preselected, and the setup screen can still change it. */
    @Test
    fun quizSetup_fromHome_setCanBeChanged() {
        show { UnolingoNavHost() }
        waitForText("2136 car.")
        compose.onAllNodesWithText("Kanji").onFirst().performClick()
        compose.onAllNodesWithText("Commencer").onFirst().performClick()
        waitForText("Réglages du quiz")
        waitForText("Grade des kanji")
        compose.onAllNodesWithText("Katakana").onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Grade des kanji").assertCountEquals(0)
        compose.onAllNodesWithText("Kanji").onFirst().performClick()
        waitForText("Grade des kanji")
    }

    /** Lets the quiz's 2 s delay elapse (the view model's scope runs on the main looper). */
    private fun waitForAutoAdvance() {
        compose.runOnUiThread {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS + 100))
        }
        compose.waitForIdle()
    }

    @Test
    fun displayQuiz_questionFeedbackAndResult() {
        show { QuizScreen(QuizConfig(CharacterSet.HIRAGANA, help = true), onBack = {}) }
        waitForText("Question 1 / 10")
        capture("quiz-question")
        answerCurrentQuestion()
        capture("quiz-feedback")
        // No "next" button: the series moves on by itself, and a tap during the delay is ignored.
        answerCurrentQuestion()
        waitForAutoAdvance()
        waitForText("Question 2 / 10")
        repeat(9) {
            answerCurrentQuestion()
            waitForAutoAdvance()
        }
        waitForText("Série terminée")
        capture("quiz-result")
        compose.onAllNodesWithText("Rejouer").onFirst().performClick()
        waitForText("Question 1 / 10")
    }

    private val sample = listOf(
        JapaneseCharacter("め", listOf("me"), similar = setOf("ぬ")),
        JapaneseCharacter("ぬ", listOf("nu"), similar = setOf("め")),
        JapaneseCharacter("ね", listOf("ne")),
        JapaneseCharacter("の", listOf("no")),
    )

    private fun quizState(feedbackAnswer: String?, help: String? = null): QuizUiState.Answering {
        val question = Question.RomajiToCharacter(sample[1], "nu", sample, help = help)
        val outcomes = listOf(true, false, true, null, null, null, null, null, null, null)
        return QuizUiState.Answering(
            number = 4,
            total = 10,
            score = 2,
            question = question,
            feedback = feedbackAnswer?.let { QuestionResult(question, it, isCorrect = it == "ぬ") },
            outcomes = if (feedbackAnswer == null) outcomes else outcomes.toMutableList().also { it[3] = feedbackAnswer == "ぬ" },
        )
    }

    @Test
    fun quizDots_mixedStatesAnswerGivenAndHelp() {
        var state by mutableStateOf<QuizUiState>(quizState(null, help = "mouton\nnu"))
        show {
            QuizContent(
                title = "Hiragana",
                subtitle = "Hiragana · Affichage",
                uiState = state,
                isSpeaking = false,
                actions = QuizActions.None,
                onBack = {},
            )
        }
        capture("quiz-dots-in-progress")
        compose.runOnUiThread { state = quizState("め", help = "mouton\nnu") }
        capture("quiz-dots-answer-wrong")
        compose.runOnUiThread { state = quizState("ぬ") }
        capture("quiz-dots-answer-right")
        compose.onAllNodesWithContentDescription("Question 1 : correcte").onFirst().assertExists()
        compose.onAllNodesWithContentDescription("Question 2 : incorrecte").onFirst().assertExists()
        compose.onAllNodesWithContentDescription("Question 5 : pas encore répondue").onFirst().assertExists()
    }

    @Test
    fun kanjiQuiz() {
        show { QuizScreen(QuizConfig(CharacterSet.KANJI, help = true), onBack = {}) }
        waitForText("Question 1 / 10")
        answerCurrentQuestion()
        capture("quiz-kanji-feedback")
    }

    @Test
    fun kanjiMeaningQuiz() {
        show { QuizScreen(QuizConfig(CharacterSet.KANJI, QuestionKind.CHARACTER_TO_MEANING, help = true), onBack = {}) }
        waitForText("Question 1 / 10")
        capture("quiz-kanji-meaning")
        answerCurrentQuestion()
        capture("quiz-kanji-meaning-feedback")
    }

    @Test
    fun audioQuiz() {
        show { QuizScreen(QuizConfig(CharacterSet.KATAKANA, tts = true), onBack = {}) }
        // Robolectric has no real TTS engine: depending on its shadow, this shows the questions or an audio error.
        compose.waitUntil(10_000) {
            listOf("Question 1 / 10", "Réessayer").any { compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        }
        capture("quiz-audio")
    }

    /** Waits for the card counter, or for a voice problem: Robolectric's TTS shadow decides which one shows. */
    private fun waitForLearnCard(counter: String) = compose.waitUntil(10_000) {
        listOf(counter, "Réessayer").any { compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun learn_kanaCardsAndNavigation() {
        show { LearnScreen(CharacterSet.HIRAGANA, onBack = {}) }
        waitForLearnCard("1 / 104")
        capture("learn")
        if (compose.onAllNodesWithText("1 / 104").fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodesWithContentDescription("Fiche suivante").onFirst().performClick()
            waitForText("2 / 104")
            capture("learn-next")
        }
    }

    @Test
    @Config(qualifiers = "+night")
    fun learnDark() {
        show { LearnScreen(CharacterSet.KATAKANA, onBack = {}) }
        waitForLearnCard("1 / 104")
        capture("learn-dark")
    }

    @Test
    fun learn_kanjiCard() {
        show { LearnScreen(CharacterSet.KANJI, onBack = {}) }
        waitForLearnCard("1 / 2136")
        capture("learn-kanji")
        if (compose.onAllNodesWithText("1 / 2136").fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodesWithText("Grade 2").onFirst().performClick()
            waitForText("1 / 160")
            capture("learn-kanji-grade2")
        }
    }

    @Test
    fun perfectScore() {
        show {
            Surface {
                Hanamaru(color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(260.dp)) {
                    Text("10 / 10", style = MaterialTheme.typography.displayMedium)
                }
            }
        }
        capture("hanamaru")
    }

    @Test
    fun battle_setupGameAndAnswer() {
        show { BattleScreen(onBack = {}) }
        capture("battle-setup")
        compose.onAllNodesWithText("Commencer le duel").onFirst().performClick()
        waitForText("Joueur 1")
        capture("battle-game")
        // Player 1's choices come after player 2's in the tree: index 4 is player 1's first choice.
        compose.onAllNodesWithTag(ChoiceTestTag)[4].performClick()
        capture("battle-answer")
    }

    @Test
    @Config(qualifiers = "+night")
    fun battleDark() {
        show { BattleScreen(onBack = {}) }
        compose.onAllNodesWithText("Commencer le duel").onFirst().performClick()
        waitForText("Joueur 1")
        compose.onAllNodesWithTag(ChoiceTestTag)[4].performClick()
        capture("battle-dark")
    }
}
