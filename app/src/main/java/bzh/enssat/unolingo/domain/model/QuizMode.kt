package bzh.enssat.unolingo.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

enum class QuestionType {
    /** Show a character, the user types its reading. */
    CHARACTER_TO_ROMAJI,

    /** Show a character, the user picks its reading among choices (used where typing is impractical, e.g. battle). */
    CHARACTER_TO_ROMAJI_CHOICE,

    /** Show a reading, the user picks the matching character. */
    ROMAJI_TO_CHARACTER,

    /** The app pronounces a reading (TTS), the user picks the character. */
    SOUND_TO_CHARACTER,

    /** Show a character, the user listens to several pronunciations (TTS) and picks the right one. */
    CHARACTER_TO_SOUND,
}

// Used as navigation arguments: keep the serializer names stable under minification.
@Keep
@Serializable
enum class QuizMode(val questionCount: Int, val questionTypes: List<QuestionType>) {
    DISPLAY(
        questionCount = 10,
        questionTypes = listOf(QuestionType.CHARACTER_TO_ROMAJI, QuestionType.ROMAJI_TO_CHARACTER),
    ),
    AUDIO(
        questionCount = 20,
        questionTypes = listOf(QuestionType.SOUND_TO_CHARACTER, QuestionType.CHARACTER_TO_SOUND),
    ),
}
