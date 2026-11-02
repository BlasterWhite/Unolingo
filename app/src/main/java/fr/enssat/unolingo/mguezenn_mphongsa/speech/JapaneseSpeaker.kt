package fr.enssat.unolingo.mguezenn_mphongsa.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import fr.enssat.unolingo.mguezenn_mphongsa.domain.speech.SpeechPace
import java.util.Locale

enum class SpeakerStatus { INITIALIZING, READY, MISSING_VOICE, UNAVAILABLE }

/** Speech rates passed to [JapaneseSpeaker.speak]. */
object SpeechRates {
    /** Slightly slower than normal, for learners (the quiz pace). */
    const val DEFAULT = 0.85f
    const val NORMAL = 1.0f
    const val SLOW = 0.5f

    fun of(pace: SpeechPace): Float = if (pace == SpeechPace.NORMAL) NORMAL else SLOW
}

/** Pronounces Japanese text. Call [release] when done. */
interface JapaneseSpeaker {
    val status: StateFlow<SpeakerStatus>

    /** True while something is being pronounced. */
    val isSpeaking: StateFlow<Boolean>

    /**
     * Interrupts whatever is being said. Ignored unless [status] is [SpeakerStatus.READY].
     * [rate] is the TTS speech rate (1.0 = normal); the default is the quiz's learner pace.
     */
    fun speak(text: String, rate: Float = SpeechRates.DEFAULT)

    fun release()
}

class AndroidJapaneseSpeaker(context: Context) : JapaneseSpeaker {
    private val _status = MutableStateFlow(SpeakerStatus.INITIALIZING)
    override val status: StateFlow<SpeakerStatus> = _status.asStateFlow()
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { initStatus ->
        _status.value = if (initStatus == TextToSpeech.SUCCESS) configure() else SpeakerStatus.UNAVAILABLE
    }

    private fun configure(): SpeakerStatus = when (tts.setLanguage(Locale.JAPANESE)) {
        // The engine works but has no Japanese voice installed (it can usually be downloaded).
        TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> SpeakerStatus.MISSING_VOICE
        else -> {
            // Called on a TTS binder thread; MutableStateFlow is thread-safe.
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            SpeakerStatus.READY
        }
    }

    override fun speak(text: String, rate: Float) {
        if (_status.value != SpeakerStatus.READY) return
        tts.setSpeechRate(rate)
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) == TextToSpeech.ERROR) {
            Log.w(TAG, "Cannot speak '$text'")
        }
    }

    override fun release() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "JapaneseSpeaker"
    }
}
