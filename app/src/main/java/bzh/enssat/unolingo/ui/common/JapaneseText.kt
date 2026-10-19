package bzh.enssat.unolingo.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.TextUnit

// Without a Japanese locale, a device set to French may draw kanji with their Chinese glyph variants.
private val JapaneseLocale = LocaleList("ja-JP")

/** Text containing Japanese characters (kana or kanji). */
@Composable
fun JapaneseText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = style.copy(localeList = JapaneseLocale),
    )
}
