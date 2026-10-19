package bzh.enssat.unolingo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// extraSmall: progress bar · small: text field, chips · medium: cards, correction card · large: choice buttons ·
// extraLarge: prompt panels, sheets, dialogs. Buttons and segmented selectors keep Material's pill shape.
val UnolingoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
