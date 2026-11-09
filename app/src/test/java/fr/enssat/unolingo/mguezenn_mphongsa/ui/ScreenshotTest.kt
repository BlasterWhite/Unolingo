package fr.enssat.unolingo.mguezenn_mphongsa.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.ui.battle.BattleScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.ChoiceTestTag
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.Hanamaru
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.HomeScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.learn.LearnScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

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
        show { HomeScreen(onStartQuiz = {}, onLearn = {}, onOpenStats = {}, onOpenBattle = {}) }
        waitForText("104 car.")
        capture("home")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeDark() {
        show { HomeScreen(onStartQuiz = {}, onLearn = {}, onOpenStats = {}, onOpenBattle = {}) }
        waitForText("104 car.")
        capture("home-dark")
    }

    @Test
    fun displayQuiz_questionFeedbackAndResult() {
        show { QuizScreen(QuizConfig(CharacterSet.HIRAGANA, help = true), onBack = {}) }
        waitForText("Question 1 / 10")
        capture("quiz-question")
        answerCurrentQuestion()
        capture("quiz-feedback")
        repeat(10) { index ->
            if (index > 0) answerCurrentQuestion()
            val next = if (index == 9) "Voir le résultat" else "Question suivante"
            compose.onAllNodesWithText(next).onFirst().performClick()
        }
        waitForText("Série terminée")
        capture("quiz-result")
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
