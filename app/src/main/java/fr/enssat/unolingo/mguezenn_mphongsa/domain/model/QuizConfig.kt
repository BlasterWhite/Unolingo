package fr.enssat.unolingo.mguezenn_mphongsa.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * What the user wants to be asked (TP2). "Reading" is the romaji for kana and the hiragana reading for kanji.
 * [MIX] spreads every kind available for the set.
 */
// Used as navigation arguments: keep the serializer names stable under minification.
@Keep
@Serializable
enum class QuestionKind {
    CHARACTER_TO_READING,
    READING_TO_CHARACTER,
    CHARACTER_TO_MEANING,
    MEANING_TO_CHARACTER,
    MIX;

    companion object {
        /** Kana have no meaning questions; kanji have all four kinds. [MIX] is always offered, last. */
        fun availableFor(set: CharacterSet): List<QuestionKind> = when (set.family) {
            CharacterFamily.PHONETIC -> listOf(CHARACTER_TO_READING, READING_TO_CHARACTER, MIX)
            CharacterFamily.IDEOGRAPHIC -> entries
        }
    }
}

/**
 * Settings of one quiz, passed in the navigation route.
 *
 * - A quiz is always [QUESTION_COUNT] questions with 4 choices; typed answers no longer exist.
 * - [grade]: for kanji, the quiz draws the kanji of grade **≤ [grade]** (the TP text says ≥, the user chose ≤).
 *   Kana quizzes always use grade 1 and ignore it.
 * - [help]: questions carry a help text (see `QuestionHelp`).
 * - [tts]: reading questions become audio questions; meaning questions stay visual but the kanji reading is spoken.
 */
@Keep
@Serializable
data class QuizConfig(
    val characterSet: CharacterSet,
    val questionKind: QuestionKind = QuestionKind.MIX,
    val help: Boolean = false,
    val tts: Boolean = false,
    val grade: Int = defaultGrade(characterSet),
) {
    init {
        require(questionKind in QuestionKind.availableFor(characterSet)) { "$questionKind is not offered for $characterSet" }
        require(grade in GRADES) { "Unknown grade $grade" }
        require(characterSet == CharacterSet.KANJI || grade == KANA_GRADE) { "A kana quiz has grade $KANA_GRADE" }
    }

    /** Question types of this quiz, to be spread evenly over its questions. */
    val questionTypes: List<QuestionType>
        get() {
            val kinds = if (questionKind == QuestionKind.MIX) {
                QuestionKind.availableFor(characterSet).filter { it != QuestionKind.MIX }
            } else {
                listOf(questionKind)
            }
            return kinds.map(::typeOf)
        }

    private fun typeOf(kind: QuestionKind): QuestionType = when (kind) {
        QuestionKind.CHARACTER_TO_READING ->
            if (tts) QuestionType.CHARACTER_TO_SOUND else QuestionType.CHARACTER_TO_ROMAJI_CHOICE
        QuestionKind.READING_TO_CHARACTER ->
            if (tts) QuestionType.SOUND_TO_CHARACTER else QuestionType.ROMAJI_TO_CHARACTER
        QuestionKind.CHARACTER_TO_MEANING -> QuestionType.CHARACTER_TO_MEANING
        QuestionKind.MEANING_TO_CHARACTER -> QuestionType.MEANING_TO_CHARACTER
        QuestionKind.MIX -> error("MIX is expanded before")
    }

    companion object {
        const val QUESTION_COUNT = 10

        const val KANA_GRADE = 1

        /** Grades of the kanji data: 1 to 6 are school grades, 8 the other jōyō kanji. */
        val GRADES: List<Int> = listOf(1, 2, 3, 4, 5, 6, 8)

        /** Kanji grade used when none is chosen: the 240 kanji of grades 1 and 2. */
        const val DEFAULT_KANJI_GRADE = 2

        fun defaultGrade(set: CharacterSet): Int = if (set == CharacterSet.KANJI) DEFAULT_KANJI_GRADE else KANA_GRADE
    }
}
