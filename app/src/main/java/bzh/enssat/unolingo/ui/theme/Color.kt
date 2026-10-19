package bzh.enssat.unolingo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "Hanamaru" art direction (docs/ui): seiji teal primary, yamabuki gold tertiary, washi paper surfaces.
// Roles carry meaning across the app: primaryContainer = right answer, errorContainer = wrong answer,
// secondaryContainer = selected choice, primary = player 1, tertiary = player 2.

val UnolingoLightColors = lightColorScheme(
    primary = Color(0xFF006A66), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA3EFE6), onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A5A92), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE1FF), onSecondaryContainer = Color(0xFF0B1A4A),
    tertiary = Color(0xFF7A5900), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA1), onTertiaryContainer = Color(0xFF261900),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8F2), onBackground = Color(0xFF1C1B17),
    surface = Color(0xFFFBF8F2), onSurface = Color(0xFF1C1B17),
    surfaceVariant = Color(0xFFEAE4D8), onSurfaceVariant = Color(0xFF4A463D),
    outline = Color(0xFF7C776C), outlineVariant = Color(0xFFCDC6B9),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F2EA),
    surfaceContainer = Color(0xFFF0ECE4), surfaceContainerHigh = Color(0xFFEAE6DE),
    surfaceContainerHighest = Color(0xFFE5E1D9),
    inverseSurface = Color(0xFF31302C), inverseOnSurface = Color(0xFFF4F0E8),
    inversePrimary = Color(0xFF82D5CB),
)

val UnolingoDarkColors = darkColorScheme(
    primary = Color(0xFF82D5CB), onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504C), onPrimaryContainer = Color(0xFFA3EFE6),
    secondary = Color(0xFFB9C3FF), onSecondary = Color(0xFF1A2C61),
    secondaryContainer = Color(0xFF33427A), onSecondaryContainer = Color(0xFFDDE1FF),
    tertiary = Color(0xFFF0BF4C), onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4200), onTertiaryContainer = Color(0xFFFFDEA1),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF151412), onBackground = Color(0xFFE7E2D9),
    surface = Color(0xFF151412), onSurface = Color(0xFFE7E2D9),
    surfaceVariant = Color(0xFF4A463D), onSurfaceVariant = Color(0xFFCDC6B9),
    outline = Color(0xFF979084), outlineVariant = Color(0xFF4A463D),
    surfaceContainerLowest = Color(0xFF0F0E0D), surfaceContainerLow = Color(0xFF1D1C1A),
    surfaceContainer = Color(0xFF21201E), surfaceContainerHigh = Color(0xFF2C2A28),
    surfaceContainerHighest = Color(0xFF373532),
    inverseSurface = Color(0xFFE7E2D9), inverseOnSurface = Color(0xFF31302C),
    inversePrimary = Color(0xFF006A66),
)
