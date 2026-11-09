package fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz

import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.QuestionAnswer
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.ResultsRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.Question
import fr.enssat.unolingo.mguezenn_mphongsa.sound.AnswerSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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

    private class Saved(
        val config: QuizConfig,
        val score: Int,
        val total: Int,
        val playedAt: Long,
        val answers: List<QuestionAnswer>,
    )

    private val saved = mutableListOf<Saved>()

    private val results = object : ResultsRepository {
        override suspend fun insert(config: QuizConfig, score: Int, total: Int, playedAt: Long, answers: List<QuestionAnswer>): Long {
            saved += Saved(config, score, total, playedAt, answers)
            return saved.size.toLong()
        }

        override fun observeOutcomes(): Flow<List<QuizOutcome>> = emptyFlow()
        override suspend fun latestOutcomes(count: Int): List<QuizOutcome> = emptyList()
        override suspend fun insertFake(outcomes: List<QuizOutcome>) = Unit
        override suspend fun deleteFake() = Unit
    }

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): QuizViewModel {
        val config = QuizConfig(CharacterSet.HIRAGANA, QuestionKind.READING_TO_CHARACTER)
        return QuizViewModel(
            config,
            repository,
            results,
            AnswerSoundPlayer { played += it },
            ioDispatcher = dispatcher,
            clock = { 42L },
        ).also { scheduler.runCurrent() }
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

    private fun QuizViewModel.answerAll(rightOnes: Int) {
        repeat(10) { index ->
            val answer = if (index < rightOnes) rightAnswer() else "not a character"
            submitAnswer(answer)
            advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS)
        }
    }

    @Test
    fun finishedQuiz_isSavedOnceWithItsContent() {
        val vm = viewModel()
        vm.answerAll(rightOnes = 7)
        assertTrue(vm.uiState.value is QuizUiState.Finished)
        advance(60_000)
        assertEquals(1, saved.size)
        val result = saved.single()
        assertEquals(QuizConfig(CharacterSet.HIRAGANA, QuestionKind.READING_TO_CHARACTER), result.config)
        assertEquals(7, result.score)
        assertEquals(10, result.total)
        assertEquals(42L, result.playedAt)
        assertEquals(10, result.answers.size)
        assertEquals(7, result.answers.count { it.correct })
    }

    @Test
    fun finishedQuiz_isNotSavedAgainWhenTimePasses() {
        val vm = viewModel()
        vm.answerAll(rightOnes = 10)
        advance(120_000)
        assertEquals(1, saved.size)
    }

    @Test
    fun replay_savesTheNextQuizAsANewResult() {
        val vm = viewModel()
        vm.answerAll(rightOnes = 10)
        vm.restart()
        advance(1)
        vm.answerAll(rightOnes = 0)
        advance(60_000)
        assertEquals(listOf(10, 0), saved.map { it.score })
    }

    @Test
    fun abandonedQuiz_isNotSaved() {
        val vm = viewModel()
        repeat(5) {
            vm.submitAnswer(vm.rightAnswer())
            advance(QuizViewModel.NEXT_QUESTION_DELAY_MILLIS)
        }
        vm.submitAnswer(vm.rightAnswer())
        vm.viewModelScope.cancel()
        advance(60_000)
        assertTrue(saved.isEmpty())
    }
}
