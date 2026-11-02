package fr.enssat.unolingo.mguezenn_mphongsa.domain.speech

/** Pace of one listening. */
enum class SpeechPace { NORMAL, SLOW }

/**
 * Alternates the pace of successive listenings of the same text, like Google Translate:
 * normal, slow, normal, slow… The sequence restarts at normal when the text changes.
 */
class SpeechRateAlternator {
    private var lastText: String? = null
    private var nextPace = SpeechPace.NORMAL

    /** Pace to use for this listening of [text]; advances the alternation. */
    fun next(text: String): SpeechPace {
        if (text != lastText) {
            lastText = text
            nextPace = SpeechPace.NORMAL
        }
        val pace = nextPace
        nextPace = if (pace == SpeechPace.NORMAL) SpeechPace.SLOW else SpeechPace.NORMAL
        return pace
    }

    fun reset() {
        lastText = null
        nextPace = SpeechPace.NORMAL
    }
}
