package fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.Question
import fr.enssat.unolingo.mguezenn_mphongsa.sound.AnswerSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val played = mutableListOf<Boolean>()

    private val characters = listOf(
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "さ" to "sa", "す" to "su", "せ" to "se", "そ" to "so", "た" to "ta",
    ).map { (symbol, reading) -> JapaneseCharacter(symbol, listOf(reading)) }

    private val repository = object : CharacterRepository {
        override suspend fun getCharacters(set: CharacterSet) = characters
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher(scheduler))

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): QuizViewModel {
        val config = QuizConfig(CharacterSet.HIRAGANA, QuestionKind.READING_TO_CHARACTER)
        return QuizViewModel(config, repository, AnswerSoundPlayer { played += it }).also { scheduler.runCurrent() }
    }

    private fun QuizViewModel.answering() = uiState.value as QuizUiState.Answering

    private fun QuizViewModel.rightAnswer() = (answering().question as Question.RomajiToCharacter).character.symbol

    private fun advance(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

    @Test
    fun answer_updatesResultsAndKeepsTheQuestionDuringTheDelay() {
        val vm = viewModel()
        assertEquals(List(10) { null }, vm.answering().outcomes)
        vm.submitAnswer(vm.rightAnswer())
        advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS - 1)
        val state = vm.answering()
        assertEquals(1, state.number)
        assertNotNull(state.feedback)
        assertEquals(1, state.score)
        assertEquals(true, state.outcomes[0])
        assertNull(state.outcomes[1])
        assertEquals(listOf(true), played)
    }

    @Test
    fun answer_movesToNextQuestionAfterTwoSeconds() {
        val vm = viewModel()
        vm.submitAnswer("not a character")
        advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS)
        val state = vm.answering()
        assertEquals(2, state.number)
        assertNull(state.feedback)
        assertEquals(false, state.outcomes[0])
        assertNull(state.outcomes[1])
        assertEquals(0, state.score)
    }

    @Test
    fun delayIsTwoSeconds() = assertEquals(2_000L, QuizViewModel.NEXT_QUESTION_DELAY_MILLIS)

    @Test
    fun clicksDuringTheDelayAreIgnored() {
        val vm = viewModel()
        vm.submitAnswer("not a character")
        advance(500)
        vm.submitAnswer(vm.rightAnswer())
        vm.submitAnswer(vm.rightAnswer())
        advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS - 500)
        val state = vm.answering()
        assertEquals(2, state.number)
        assertEquals(0, state.score)
        assertEquals(listOf(false), played)
        // The ignored taps did not push the next advance further away or answer the next question.
        assertNull(state.feedback)
    }

    @Test
    fun fullSeries_finishesAfterTheLastDelayAndReplayRestartsWithTheSameConfig() {
        val vm = viewModel()
        repeat(10) { index ->
            assertEquals(index + 1, vm.answering().number)
            vm.submitAnswer(if (index % 2 == 0) vm.rightAnswer() else "x")
            advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS)
        }
        val finished = vm.uiState.value as QuizUiState.Finished
        assertEquals(5, finished.score)
        assertEquals(10, finished.total)
        assertEquals(5, finished.mistakes.size)

        vm.restart()
        val state = vm.answering()
        assertEquals(1, state.number)
        assertEquals(10, state.total)
        assertEquals(0, state.score)
        assertTrue(state.question is Question.RomajiToCharacter)
        assertEquals(List(10) { null }, state.outcomes)
    }

    @Test
    fun restart_cancelsThePendingAdvance() {
        val vm = viewModel()
        vm.submitAnswer("x")
        advance(1_000)
        vm.restart()
        advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS)
        assertEquals(1, vm.answering().number)
    }

    @Test
    fun clearedViewModel_neverAdvances() {
        val vm = viewModel()
        vm.submitAnswer("x")
        // ViewModel.clear() is internal: cancel the scope the way the framework does.
        vm.viewModelScope.cancel()
        advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS * 2)
        assertNotNull(vm.answering().feedback)
    }
}
