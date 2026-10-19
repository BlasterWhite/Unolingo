package bzh.enssat.unolingo.ui

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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.QuizMode
import bzh.enssat.unolingo.ui.battle.BattleScreen
import bzh.enssat.unolingo.ui.common.ChoiceTestTag
import bzh.enssat.unolingo.ui.common.Hanamaru
import bzh.enssat.unolingo.ui.home.HomeScreen
import bzh.enssat.unolingo.ui.quiz.QuizScreen
import bzh.enssat.unolingo.ui.theme.UnolingoTheme
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
        val fields = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        if (fields.isNotEmpty()) {
            compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("ka")
            compose.onAllNodesWithText("Valider").onFirst().performClick()
        } else {
            compose.onAllNodesWithTag(ChoiceTestTag).onFirst().performClick()
        }
    }

    @Test
    fun home() {
        show { HomeScreen(onStartQuiz = { _, _ -> }, onOpenStats = {}, onOpenBattle = {}) }
        waitForText("104 car.")
        capture("home")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeDark() {
        show { HomeScreen(onStartQuiz = { _, _ -> }, onOpenStats = {}, onOpenBattle = {}) }
        waitForText("104 car.")
        capture("home-dark")
    }

    @Test
    fun displayQuiz_questionFeedbackAndResult() {
        show { QuizScreen(CharacterSet.HIRAGANA, QuizMode.DISPLAY, onBack = {}) }
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
        show { QuizScreen(CharacterSet.KANJI, QuizMode.DISPLAY, onBack = {}) }
        waitForText("Question 1 / 10")
        answerCurrentQuestion()
        capture("quiz-kanji-feedback")
    }

    @Test
    fun audioQuiz() {
        show { QuizScreen(CharacterSet.KATAKANA, QuizMode.AUDIO, onBack = {}) }
        // Robolectric has no real TTS engine: depending on its shadow, this shows the questions or an audio error.
        compose.waitUntil(10_000) {
            listOf("Question 1 / 20", "Réessayer").any { compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        }
        capture("quiz-audio")
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
