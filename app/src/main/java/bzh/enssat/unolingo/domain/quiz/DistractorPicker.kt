package bzh.enssat.unolingo.domain.quiz

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import kotlin.random.Random

/** Picks wrong choices for a multiple-choice question, preferring characters that look or sound alike. */
internal object DistractorPicker {

    /**
     * Wrong characters for a "romaji → character" or "sound → character" question on [prompt]. With [bySound],
     * homophones of [target] are excluded too (see [soundsAlike]).
     */
    fun pickCharacters(
        target: JapaneseCharacter,
        prompt: String,
        pool: List<JapaneseCharacter>,
        count: Int,
        random: Random,
        bySound: Boolean = false,
    ): List<JapaneseCharacter> =
        rank(target, pool, random)
            // A character that also reads as the prompt (じ/ぢ, 日/火 for "ka") would be a second right answer.
            .filterNot { AnswerChecker.acceptsReading(it, prompt) }
            .filterNot { bySound && soundsAlike(target, it) }
            .take(count)

    /**
     * Wrong readings for a "character → romaji" or "character → sound" question: none of them is accepted for
     * [target]. With [bySound], readings of homophones of [target] are excluded too (see [soundsAlike]).
     */
    fun pickReadings(
        target: JapaneseCharacter,
        pool: List<JapaneseCharacter>,
        count: Int,
        random: Random,
        bySound: Boolean = false,
    ): List<String> =
        rank(target, pool, random)
            .filterNot { bySound && soundsAlike(target, it) }
            .mapNotNull { candidate ->
                candidate.readings.shuffled(random).firstOrNull { !AnswerChecker.acceptsReading(target, it) }
            }
            .distinctBy { AnswerChecker.normalizeRomaji(it) }
            .take(count)

    /**
     * Two characters can be pronounced the same when they share an accepted answer: を ("wo", also "o") sounds
     * like お, ぢ like じ, づ like ず; for kanji, any common reading.
     */
    private fun soundsAlike(a: JapaneseCharacter, b: JapaneseCharacter): Boolean =
        a.acceptedAnswers.any { AnswerChecker.acceptsReading(b, it) }

    private fun rank(target: JapaneseCharacter, pool: List<JapaneseCharacter>, random: Random) =
        pool
            .filter { it.symbol != target.symbol }
            .shuffled(random)
            .sortedByDescending { similarity(target, it) } // stable: ties keep their random order

    private fun similarity(a: JapaneseCharacter, b: JapaneseCharacter): Int {
        var score = 0
        if (b.symbol in a.similar || a.symbol in b.similar) score += 3
        val readingA = a.readings.first()
        val readingB = b.readings.first()
        if (readingA.last() == readingB.last()) score += 1 // same vowel
        if (readingA.dropLast(1) == readingB.dropLast(1)) score += 1 // same consonant
        if (a.symbol.length == b.symbol.length) score += 1 // yōon against yōon
        return score
    }
}
