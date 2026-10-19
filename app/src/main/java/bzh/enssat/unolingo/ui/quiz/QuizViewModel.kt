package bzh.enssat.unolingo.ui.quiz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bzh.enssat.unolingo.UnolingoApplication
import bzh.enssat.unolingo.data.CharacterRepository
import bzh.enssat.unolingo.domain.kana.KanaConverter
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuizMode
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.domain.quiz.QuestionGenerator
import bzh.enssat.unolingo.domain.quiz.QuestionResult
import bzh.enssat.unolingo.domain.quiz.QuizSession
import bzh.enssat.unolingo.speech.AndroidJapaneseSpeaker
import bzh.enssat.unolingo.speech.JapaneseSpeaker
import bzh.enssat.unolingo.speech.SpeakerStatus
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

    /** [number] is 1-based. [feedback] is set once the current question has been answered. */
    data class Answering(
        val number: Int,
        val total: Int,
        val score: Int,
        val question: Question,
        val feedback: QuestionResult?,
    ) : QuizUiState

    data class Finished(val score: Int, val total: Int, val mistakes: List<QuestionResult>) : QuizUiState
}

/** [speakerFactory] is only needed for modes with audio questions. */
class QuizViewModel(
    private val characterSet: CharacterSet,
    private val mode: QuizMode,
    private val repository: CharacterRepository,
    private val speakerFactory: (() -> JapaneseSpeaker)? = null,
    private val generator: QuestionGenerator = QuestionGenerator(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var characters: List<JapaneseCharacter> = emptyList()
    private var session: QuizSession? = null
    private var speaker: JapaneseSpeaker? = null
    private var kanaConverter: KanaConverter? = null

    init {
        viewModelScope.launch {
            try {
                characters = repository.getCharacters(characterSet)
                if (speakerFactory != null) {
                    kanaConverter = KanaConverter(repository.getCharacters(CharacterSet.HIRAGANA))
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
        val newSession = QuizSession(generator.generate(characters, mode))
        session = newSession
        showCurrentQuestion(newSession)
    }

    fun submitAnswer(answer: String) {
        val current = session ?: return
        val state = _uiState.value
        if (state !is QuizUiState.Answering || state.feedback != null) return
        val updated = current.submit(answer)
        session = updated
        _uiState.value = state.copy(score = updated.score, feedback = updated.results.last())
    }

    fun next() {
        val current = session ?: return
        if (current.isFinished) {
            _uiState.value = QuizUiState.Finished(
                score = current.score,
                total = current.questions.size,
                mistakes = current.results.filterNot { it.isCorrect },
            )
        } else {
            showCurrentQuestion(current)
        }
    }

    /** Pronounces the reading of the current "sound → character" question again. */
    fun replay() {
        val question = (_uiState.value as? QuizUiState.Answering)?.question as? Question.SoundToCharacter ?: return
        speakReading(question.prompt, fallback = question.character.symbol)
    }

    /** Pronounces one of the proposals of a "character → sound" question. */
    fun playReading(reading: String) {
        speakReading(reading, fallback = reading)
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
        )
        if (question is Question.SoundToCharacter) speakReading(question.prompt, fallback = question.character.symbol)
    }

    /** Speaks [reading] written in hiragana, so the engine says exactly this reading (not another one of a kanji). */
    private fun speakReading(reading: String, fallback: String) {
        speaker?.speak(kanaConverter?.toHiragana(reading) ?: fallback)
    }

    private fun onLoadError(e: Exception) {
        Log.e(TAG, "Cannot load $characterSet characters", e)
        _uiState.value = QuizUiState.LoadError
    }

    companion object {
        private const val TAG = "QuizViewModel"
        private const val SPEAKER_INIT_TIMEOUT_MILLIS = 5_000L

        fun factory(characterSet: CharacterSet, mode: QuizMode) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                QuizViewModel(
                    characterSet = characterSet,
                    mode = mode,
                    repository = app.container.characterRepository,
                    speakerFactory = if (mode == QuizMode.AUDIO) ({ AndroidJapaneseSpeaker(app) }) else null,
                )
            }
        }
    }
}
