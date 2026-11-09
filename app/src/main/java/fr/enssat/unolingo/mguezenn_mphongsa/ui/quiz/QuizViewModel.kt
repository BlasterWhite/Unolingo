package fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.unolingo.mguezenn_mphongsa.UnolingoApplication
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.QuestionAnswer
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.ResultsRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.Question
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.QuestionGenerator
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.QuestionResult
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.QuizPool
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.QuizSession
import fr.enssat.unolingo.mguezenn_mphongsa.sound.AnswerSoundPlayer
import fr.enssat.unolingo.mguezenn_mphongsa.domain.speech.SpeechRateAlternator
import fr.enssat.unolingo.mguezenn_mphongsa.domain.speech.SpokenReadings
import fr.enssat.unolingo.mguezenn_mphongsa.speech.AndroidJapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.JapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeakerStatus
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeechRates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

sealed interface QuizUiState {
    data object Loading : QuizUiState

    data object LoadError : QuizUiState

    /** Audio mode: the text-to-speech engine has no Japanese voice. */
    data object MissingVoice : QuizUiState

    /** Audio mode: no usable text-to-speech engine. */
    data object SpeechUnavailable : QuizUiState

    /**
     * [number] is 1-based. [feedback] is set once the current question has been answered. [outcomes] has one entry per
     * question: true = right, false = wrong, null = not answered yet.
     */
    data class Answering(
        val number: Int,
        val total: Int,
        val score: Int,
        val question: Question,
        val feedback: QuestionResult?,
        val outcomes: List<Boolean?> = List(total) { null },
    ) : QuizUiState

    data class Finished(val score: Int, val total: Int, val mistakes: List<QuestionResult>) : QuizUiState
}

/** [speakerFactory] is only needed when [QuizConfig.tts] is on. */
class QuizViewModel(
    private val config: QuizConfig,
    private val repository: CharacterRepository,
    private val resultsRepository: ResultsRepository,
    private val answerSounds: AnswerSoundPlayer? = null,
    private val speakerFactory: (() -> JapaneseSpeaker)? = null,
    private val generator: QuestionGenerator = QuestionGenerator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    /** Audio mode: true while a reading is being pronounced (drives the speaker animation). */
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    private var speakingJob: Job? = null

    /** Pending move to the next question after an answer; cancelled by [restart] and with the view model's scope. */
    private var advanceJob: Job? = null

    private var characters: List<JapaneseCharacter> = emptyList()
    private var session: QuizSession? = null
    private var speaker: JapaneseSpeaker? = null
    private var spokenReadings = SpokenReadings(emptyList())

    /** Successive listenings of the same reading alternate normal and slow pace, like in the Apprendre screen. */
    private val rateAlternator = SpeechRateAlternator()

    init {
        viewModelScope.launch {
            try {
                characters = QuizPool.select(config, repository.getCharacters(config.characterSet))
                spokenReadings = SpokenReadings(characters)
                if (speakerFactory != null) {
                    prepareSpeaker()
                } else {
                    restart()
                }
            } catch (e: IOException) {
                onLoadError(e)
            } catch (e: IllegalArgumentException) { // malformed JSON or invalid character entry
                onLoadError(e)
            }
        }
    }

    /** Starts a new series with freshly drawn questions. */
    fun restart() {
        if (characters.isEmpty()) return
        advanceJob?.cancel()
        val newSession = QuizSession(generator.generate(characters, config))
        session = newSession
        showCurrentQuestion(newSession)
    }

    fun submitAnswer(answer: String) {
        val current = session ?: return
        val state = _uiState.value
        if (state !is QuizUiState.Answering || state.feedback != null) return
        val updated = current.submit(answer)
        session = updated
        val result = updated.results.last()
        _uiState.value = state.copy(score = updated.score, feedback = result, outcomes = outcomesOf(updated))
        answerSounds?.play(result.isCorrect)
        // The delay lives here, not in the UI: a rotation recreates the screen but neither restarts nor replays it.
        advanceJob = viewModelScope.launch {
            delay(NEXT_QUESTION_DELAY_MILLIS)
            next()
        }
    }

    private fun next() {
        val current = session ?: return
        if (current.isFinished) {
            // Reached once per series (one pending advance at a time, none after the last question), so one save.
            saveResult(current)
            _uiState.value = QuizUiState.Finished(
                score = current.score,
                total = current.questions.size,
                mistakes = current.results.filterNot { it.isCorrect },
            )
        } else {
            showCurrentQuestion(current)
        }
    }

    /** Stores a finished quiz off the main thread; a quiz left before its end never gets here. */
    private fun saveResult(finished: QuizSession) {
        val playedAt = clock()
        val answers = finished.results.map { QuestionAnswer(it.question.character.symbol, it.isCorrect) }
        viewModelScope.launch(ioDispatcher) {
            try {
                // Not cancelled if the user leaves the result screen right away.
                withContext(NonCancellable) {
                    resultsRepository.insert(config, finished.score, finished.questions.size, playedAt, answers)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cannot save the quiz result", e)
            }
        }
    }

    /** Pronounces the reading of the current "sound → character" question again. */
    fun replay() {
        val question = (_uiState.value as? QuizUiState.Answering)?.question as? Question.SoundToCharacter ?: return
        speakReading(question.character, question.prompt)
    }

    /** Pronounces one of the proposals of a "character → sound" question. */
    fun playReading(reading: String) {
        val question = (_uiState.value as? QuizUiState.Answering)?.question as? Question.CharacterToSound ?: return
        speakReading(question.character, reading)
    }

    /** Checks the voice again, e.g. after the user installed it. No effect unless audio is unavailable. */
    fun retryVoice() {
        val state = _uiState.value
        if (state != QuizUiState.MissingVoice && state != QuizUiState.SpeechUnavailable) return
        _uiState.value = QuizUiState.Loading
        viewModelScope.launch { prepareSpeaker() }
    }

    override fun onCleared() {
        speaker?.release()
    }

    private suspend fun prepareSpeaker() {
        val factory = speakerFactory ?: return
        speaker?.release()
        val newSpeaker = factory().also { speaker = it }
        speakingJob?.cancel()
        speakingJob = viewModelScope.launch { newSpeaker.isSpeaking.collect { _isSpeaking.value = it } }
        val status = withTimeoutOrNull(SPEAKER_INIT_TIMEOUT_MILLIS) {
            newSpeaker.status.first { it != SpeakerStatus.INITIALIZING }
        }
        when (status) {
            SpeakerStatus.READY -> restart()
            SpeakerStatus.MISSING_VOICE -> _uiState.value = QuizUiState.MissingVoice
            SpeakerStatus.UNAVAILABLE, SpeakerStatus.INITIALIZING, null -> _uiState.value = QuizUiState.SpeechUnavailable
        }
    }

    private fun showCurrentQuestion(session: QuizSession) {
        val question = checkNotNull(session.currentQuestion)
        _uiState.value = QuizUiState.Answering(
            number = session.results.size + 1,
            total = session.questions.size,
            score = session.score,
            question = question,
            feedback = null,
            outcomes = outcomesOf(session),
        )
        when {
            question is Question.SoundToCharacter -> speakReading(question.character, question.prompt)
            // Meaning questions stay visual, but with tts the reading of the kanji is said when it is displayed.
            config.tts && (question is Question.CharacterToMeaning || question is Question.MeaningToCharacter) ->
                speakReading(question.character, question.character.readings.first())
        }
    }

    /**
     * Speaks [reading] (romaji) as the hiragana reading of the TP data (`kanaReadings`), so the engine says exactly
     * this reading and not another one of a kanji. [reading] may belong to another character than [character] (the
     * wrong proposals of a "character → sound" question), hence the lookup over the whole quiz pool.
     */
    private fun speakReading(character: JapaneseCharacter, reading: String) {
        val text = spokenReadings.hiraganaFor(reading) ?: character.hiraganaFor(reading) ?: character.symbol
        speaker?.speak(text, SpeechRates.of(rateAlternator.next(text)))
    }

    private fun outcomesOf(session: QuizSession): List<Boolean?> =
        List(session.questions.size) { session.results.getOrNull(it)?.isCorrect }

    private fun onLoadError(e: Exception) {
        Log.e(TAG, "Cannot load ${config.characterSet} characters", e)
        _uiState.value = QuizUiState.LoadError
    }

    companion object {
        private const val TAG = "QuizViewModel"
        private const val SPEAKER_INIT_TIMEOUT_MILLIS = 5_000L

        /** Time the right answer stays visible before the next question appears; taps are ignored meanwhile. */
        const val NEXT_QUESTION_DELAY_MILLIS = 2_000L

        fun factory(config: QuizConfig) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                QuizViewModel(
                    config = config,
                    repository = app.container.characterRepository,
                    resultsRepository = app.container.resultsRepository,
                    answerSounds = app.container.answerSounds,
                    speakerFactory = if (config.tts) ({ AndroidJapaneseSpeaker(app) }) else null,
                )
            }
        }
    }
}
