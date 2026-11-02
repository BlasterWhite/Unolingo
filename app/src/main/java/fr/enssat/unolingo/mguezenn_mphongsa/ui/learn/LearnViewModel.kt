package fr.enssat.unolingo.mguezenn_mphongsa.ui.learn

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.unolingo.mguezenn_mphongsa.UnolingoApplication
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.kana.KanaConverter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.speech.SpeechRateAlternator
import fr.enssat.unolingo.mguezenn_mphongsa.speech.AndroidJapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.JapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeakerStatus
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeechRates
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface LearnUiState {
    data object Loading : LearnUiState

    data object LoadError : LearnUiState

    /** The text-to-speech engine has no Japanese voice (same state as the audio quiz). */
    data object MissingVoice : LearnUiState

    /** No usable text-to-speech engine. */
    data object SpeechUnavailable : LearnUiState

    /** [index] is 0-based; the cards are not looped, so the first and last ones have no previous / next. */
    data class Showing(val characters: List<JapaneseCharacter>, val index: Int) : LearnUiState {
        val current: JapaneseCharacter get() = characters[index]
        val hasPrevious: Boolean get() = index > 0
        val hasNext: Boolean get() = index < characters.lastIndex
    }
}

class LearnViewModel(
    private val characterSet: CharacterSet,
    private val repository: CharacterRepository,
    private val speakerFactory: () -> JapaneseSpeaker,
    private val rateAlternator: SpeechRateAlternator = SpeechRateAlternator(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<LearnUiState>(LearnUiState.Loading)
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    /** True while a reading is being pronounced (drives the speaker button). */
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var characters: List<JapaneseCharacter>? = null
    private var index = 0
    private var loadFailed = false
    private var speakerStatus = SpeakerStatus.INITIALIZING
    private var kanaConverter: KanaConverter? = null
    private var speaker: JapaneseSpeaker? = null
    private var speakerJobs = emptyList<Job>()

    init {
        startSpeaker()
        viewModelScope.launch {
            try {
                characters = repository.getCharacters(characterSet)
                kanaConverter = KanaConverter(repository.getCharacters(CharacterSet.HIRAGANA))
            } catch (e: IOException) {
                onLoadError(e)
            } catch (e: IllegalArgumentException) { // malformed JSON or invalid character entry
                onLoadError(e)
            }
            publish()
        }
    }

    fun next() {
        val size = characters?.size ?: return
        if (index < size - 1) {
            index++
            publish()
        }
    }

    fun previous() {
        if (index > 0) {
            index--
            publish()
        }
    }

    /**
     * Pronounces the current card. Successive listenings of the same card alternate normal and slow pace;
     * the reading is spoken as hiragana so the engine never picks another reading of a kanji.
     */
    fun speak() {
        val character = (_uiState.value as? LearnUiState.Showing)?.current ?: return
        val reading = character.readings.first()
        val text = kanaConverter?.toHiragana(reading) ?: character.symbol
        speaker?.speak(text, SpeechRates.of(rateAlternator.next(text)))
    }

    /** Checks the voice again, e.g. after the user installed it. No effect unless audio is unavailable. */
    fun retryVoice() {
        if (speakerStatus != SpeakerStatus.MISSING_VOICE && speakerStatus != SpeakerStatus.UNAVAILABLE) return
        startSpeaker()
    }

    override fun onCleared() {
        speaker?.release()
    }

    private fun startSpeaker() {
        speakerJobs.forEach { it.cancel() }
        speaker?.release()
        val newSpeaker = speakerFactory().also { speaker = it }
        speakerStatus = SpeakerStatus.INITIALIZING
        rateAlternator.reset()
        speakerJobs = listOf(
            viewModelScope.launch { newSpeaker.isSpeaking.collect { _isSpeaking.value = it } },
            viewModelScope.launch {
                newSpeaker.status.collect {
                    speakerStatus = it
                    publish()
                }
            },
        )
    }

    private fun publish() {
        val loaded = characters
        _uiState.value = when {
            loadFailed -> LearnUiState.LoadError
            loaded == null -> LearnUiState.Loading
            speakerStatus == SpeakerStatus.MISSING_VOICE -> LearnUiState.MissingVoice
            speakerStatus == SpeakerStatus.UNAVAILABLE -> LearnUiState.SpeechUnavailable
            // Still initializing: the cards can already be read, speak() is ignored until the engine is ready.
            else -> LearnUiState.Showing(loaded, index)
        }
    }

    private fun onLoadError(e: Exception) {
        Log.e(TAG, "Cannot load $characterSet characters", e)
        loadFailed = true
    }

    companion object {
        private const val TAG = "LearnViewModel"

        fun factory(characterSet: CharacterSet) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                LearnViewModel(
                    characterSet = characterSet,
                    repository = app.container.characterRepository,
                    speakerFactory = { AndroidJapaneseSpeaker(app) },
                )
            }
        }
    }
}
