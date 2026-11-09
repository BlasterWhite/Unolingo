package fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.unolingo.mguezenn_mphongsa.UnolingoApplication
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.speech.AndroidJapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.JapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeakerStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Data the setup screen needs besides the settings themselves: how many kanji each grade brings, and, once the user
 * turns the text-to-speech on, whether the phone can pronounce Japanese (the quiz itself handles a missing voice too).
 */
class QuizSetupViewModel(
    private val repository: CharacterRepository,
    private val speakerFactory: () -> JapaneseSpeaker,
) : ViewModel() {

    private val _kanjiCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())

    /** Grade g → number of kanji of grade ≤ g (the pool of a quiz at that grade); empty until loaded. */
    val kanjiCounts: StateFlow<Map<Int, Int>> = _kanjiCounts.asStateFlow()

    private val _voiceStatus = MutableStateFlow<SpeakerStatus?>(null)

    /** Null until the voice has been checked (only done when the text-to-speech is wanted). */
    val voiceStatus: StateFlow<SpeakerStatus?> = _voiceStatus.asStateFlow()

    private var speaker: JapaneseSpeaker? = null
    private var statusJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val grades = repository.getCharacters(CharacterSet.KANJI).mapNotNull { it.grade }
                _kanjiCounts.value = QuizConfig.GRADES.associateWith { g -> grades.count { it <= g } }
            } catch (e: IOException) {
                Log.e(TAG, "Cannot count kanji", e) // the grade text just shows no count
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Cannot count kanji", e)
            }
        }
    }

    /** Checks the Japanese voice (again). Cheap to call on every resume: the previous engine is released first. */
    fun checkVoice() {
        statusJob?.cancel()
        speaker?.release()
        _voiceStatus.value = null
        val newSpeaker = speakerFactory().also { speaker = it }
        statusJob = viewModelScope.launch {
            newSpeaker.status.collect { _voiceStatus.value = it.takeIf { s -> s != SpeakerStatus.INITIALIZING } }
        }
    }

    /** Releases the engine when the text-to-speech is turned off. */
    fun stopVoiceCheck() {
        statusJob?.cancel()
        speaker?.release()
        speaker = null
        _voiceStatus.value = null
    }

    override fun onCleared() {
        speaker?.release()
    }

    companion object {
        private const val TAG = "QuizSetupViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                QuizSetupViewModel(app.container.characterRepository) { AndroidJapaneseSpeaker(app) }
            }
        }
    }
}
