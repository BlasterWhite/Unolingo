package bzh.enssat.unolingo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** No dynamic color (Material You): the brand palette carries meaning (right/wrong answers, players). */
@Composable
fun UnolingoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) UnolingoDarkColors else UnolingoLightColors,
        typography = UnolingoTypography,
        shapes = UnolingoShapes,
        content = content,
    )
}
