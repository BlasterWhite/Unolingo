package bzh.enssat.unolingo.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/** Phonetic sets map one character to one reading; ideographic sets can have several readings and a meaning. */
enum class CharacterFamily { PHONETIC, IDEOGRAPHIC }

// Used as navigation arguments: keep the serializer names stable under minification.
@Keep
@Serializable
enum class CharacterSet(val family: CharacterFamily) {
    HIRAGANA(CharacterFamily.PHONETIC),
    KATAKANA(CharacterFamily.PHONETIC),
    KANJI(CharacterFamily.IDEOGRAPHIC),
}
