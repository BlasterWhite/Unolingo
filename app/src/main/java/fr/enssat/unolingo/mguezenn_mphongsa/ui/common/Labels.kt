package fr.enssat.unolingo.mguezenn_mphongsa.ui.common

import androidx.annotation.StringRes
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizMode

@get:StringRes
val CharacterSet.labelRes: Int
    get() = when (this) {
        CharacterSet.HIRAGANA -> R.string.set_hiragana
        CharacterSet.KATAKANA -> R.string.set_katakana
        CharacterSet.KANJI -> R.string.set_kanji
    }

@get:StringRes
val QuizMode.labelRes: Int
    get() = when (this) {
        QuizMode.DISPLAY -> R.string.mode_display
        QuizMode.AUDIO -> R.string.mode_audio
    }
