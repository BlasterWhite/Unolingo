package bzh.enssat.unolingo.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Decorative Japanese patterns (docs/ui). Never draw them behind a character to recognize, a choice grid or text.

/**
 * Seigaiha (青海波, "waves of the blue sea"): overlapping fans of concentric arcs. [background] fills each fan so
 * that lower rows cover the upper ones. Used on the home banner and player 1's band.
 */
fun Modifier.seigaiha(lines: Color, background: Color, radius: Dp = 14.dp): Modifier = drawBehind {
    val r = radius.toPx()
    val stroke = Stroke(width = r * 0.09f)
    clipRect {
        drawRect(background)
        var row = 0
        var y = 0f
        while (y < size.height + r) {
            var x = if (row % 2 == 0) 0f else r
            while (x < size.width + r) {
                val center = Offset(x, y)
                drawCircle(background, radius = r, center = center)
                listOf(0.92f, 0.68f, 0.44f, 0.2f).forEach { drawCircle(lines, radius = r * it, center = center, style = stroke) }
                x += 2 * r
            }
            y += r / 2
            row++
        }
    }
}

/** Kanoko (鹿の子, "fawn spots"): a staggered grid of dots. Used on player 2's band. */
fun Modifier.kanoko(dots: Color, background: Color, spacing: Dp = 12.dp): Modifier = drawBehind {
    val step = spacing.toPx()
    clipRect {
        drawRect(background)
        var row = 0
        var y = step / 2
        while (y < size.height + step) {
            var x = if (row % 2 == 0) step / 2 else step
            while (x < size.width + step) {
                drawCircle(dots, radius = step * 0.13f, center = Offset(x, y))
                x += step
            }
            y += step / 2
            row++
        }
    }
}
