package fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup

import androidx.compose.runtime.saveable.Saver
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Everything the setup screen edits, kept apart from [QuizConfig] so that switching to kana and back to kanji does
 * not forget the kanji grade (a kana config has no grade). [toConfig] always yields a valid config.
 */
@Serializable
data class QuizSetupSettings(
    val characterSet: CharacterSet = CharacterSet.HIRAGANA,
    val questionKind: QuestionKind = QuestionKind.MIX,
    val help: Boolean = false,
    val tts: Boolean = false,
    val kanjiGrade: Int = QuizConfig.DEFAULT_KANJI_GRADE,
) {
    /** Kinds offered for the current set. */
    val availableKinds: List<QuestionKind>
        get() = QuestionKind.availableFor(characterSet)

    /** Changing the set keeps the other choices, except a kind the new set does not offer, which falls back to [QuestionKind.MIX]. */
    fun withSet(set: CharacterSet): QuizSetupSettings {
        val kind = if (questionKind in QuestionKind.availableFor(set)) questionKind else QuestionKind.MIX
        return copy(characterSet = set, questionKind = kind)
    }

    fun toConfig(): QuizConfig = QuizConfig(
        characterSet = characterSet,
        questionKind = if (questionKind in availableKinds) questionKind else QuestionKind.MIX,
        help = help,
        tts = tts,
        grade = if (characterSet == CharacterSet.KANJI) kanjiGrade else QuizConfig.KANA_GRADE,
    )

    companion object {
        /** Survives rotation and process death (kept as JSON, like the navigation arguments). */
        val Saver: Saver<QuizSetupSettings, String> = Saver(
            save = { Json.encodeToString(it) },
            restore = {
                try {
                    Json.decodeFromString<QuizSetupSettings>(it)
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            },
        )
    }
}
