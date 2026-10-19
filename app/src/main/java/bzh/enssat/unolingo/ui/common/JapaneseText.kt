package bzh.enssat.unolingo.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import bzh.enssat.unolingo.ui.theme.JapaneseLocale
import bzh.enssat.unolingo.ui.theme.KanaStyles
import bzh.enssat.unolingo.ui.theme.KleeOne

/** Text made of Japanese characters only (kana or kanji), in Klee One. See [KanaStyles] for sizes. */
@Composable
fun JapaneseText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = KanaStyles.Choice,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(text = text, modifier = modifier, color = color, style = style, textAlign = textAlign)
}

private val JapaneseSpan = SpanStyle(fontFamily = KleeOne, fontWeight = FontWeight.SemiBold, localeList = JapaneseLocale)

private fun Char.isJapanese(): Boolean = this in '぀'..'ヿ' || this in '一'..'鿿'

/** Appends [text], switching to Klee One for its Japanese characters (Latin text keeps the UI font). */
fun AnnotatedString.Builder.appendMixed(text: String) {
    text.forEach { char ->
        if (char.isJapanese()) withStyle(JapaneseSpan) { append(char) } else append(char)
    }
}
