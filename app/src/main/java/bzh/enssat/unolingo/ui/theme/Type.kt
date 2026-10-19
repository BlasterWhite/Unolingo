package bzh.enssat.unolingo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp
import bzh.enssat.unolingo.R

// Fonts are bundled in res/font (see tools/subset_fonts.py): Lexend for the UI and romaji, Klee One for every
// Japanese character. Klee One is never used for Latin text.

@OptIn(ExperimentalTextApi::class) // variable font axis
private fun lexend(weight: FontWeight) = Font(
    resId = R.font.lexend,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Lexend = FontFamily(lexend(FontWeight.Normal), lexend(FontWeight.SemiBold), lexend(FontWeight.Bold))

val KleeOne = FontFamily(Font(R.font.klee_one_semibold, FontWeight.SemiBold))

/** Without a Japanese locale, a device set to French may draw kanji with their Chinese glyph variants. */
val JapaneseLocale = LocaleList("ja-JP")

private fun lexendStyle(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = Lexend,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val UnolingoTypography = Typography(
    displayLarge = lexendStyle(57, 64, FontWeight.Bold, -1.0),
    displayMedium = lexendStyle(45, 52, FontWeight.SemiBold, -0.5),
    displaySmall = lexendStyle(36, 44, FontWeight.SemiBold),
    headlineLarge = lexendStyle(32, 40, FontWeight.SemiBold),
    headlineMedium = lexendStyle(28, 36, FontWeight.SemiBold),
    headlineSmall = lexendStyle(24, 32, FontWeight.SemiBold),
    titleLarge = lexendStyle(22, 28, FontWeight.SemiBold),
    titleMedium = lexendStyle(16, 24, FontWeight.SemiBold, 0.1),
    titleSmall = lexendStyle(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = lexendStyle(16, 24, FontWeight.Normal, 0.15),
    bodyMedium = lexendStyle(14, 20, FontWeight.Normal, 0.15),
    bodySmall = lexendStyle(12, 16, FontWeight.Normal, 0.2),
    labelLarge = lexendStyle(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = lexendStyle(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = lexendStyle(11, 16, FontWeight.SemiBold, 0.4),
)

/** Text styles for Japanese characters, from the largest (question prompt) to the smallest (battle choices). */
object KanaStyles {
    private fun kana(size: Int, lineHeight: Int) = TextStyle(
        fontFamily = KleeOne,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        localeList = JapaneseLocale,
    )

    val Hero = kana(96, 112)
    val Duel = kana(56, 64)
    val Choice = kana(40, 48)
    val List = kana(36, 44)
    val Compact = kana(28, 36)
}
