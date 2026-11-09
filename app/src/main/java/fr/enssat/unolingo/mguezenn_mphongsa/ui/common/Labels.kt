package fr.enssat.unolingo.mguezenn_mphongsa.ui.common

import androidx.annotation.StringRes
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind

@get:StringRes
val CharacterSet.labelRes: Int
    get() = when (this) {
        CharacterSet.HIRAGANA -> R.string.set_hiragana
        CharacterSet.KATAKANA -> R.string.set_katakana
        CharacterSet.KANJI -> R.string.set_kanji
    }

@get:StringRes
val QuestionKind.labelRes: Int
    get() = when (this) {
        QuestionKind.CHARACTER_TO_READING -> R.string.kind_character_to_reading
        QuestionKind.READING_TO_CHARACTER -> R.string.kind_reading_to_character
        QuestionKind.CHARACTER_TO_MEANING -> R.string.kind_character_to_meaning
        QuestionKind.MEANING_TO_CHARACTER -> R.string.kind_meaning_to_character
        QuestionKind.MIX -> R.string.kind_mix
    }
