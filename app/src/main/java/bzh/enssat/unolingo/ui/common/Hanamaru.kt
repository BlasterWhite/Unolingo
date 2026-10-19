package bzh.enssat.unolingo.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Hanamaru (花丸): the flower-shaped circle Japanese teachers draw on a perfect paper. Reserved for a perfect
 * series. It traces itself in 700 ms, the only animation of the app longer than 300 ms.
 */
@Composable
fun Hanamaru(color: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val center = this.center
            val radius = min(size.width, size.height) / 2 - strokeWidth
            // Petals: radius bumps with cusps between them, drawn as one continuous stroke.
            val flower = Path()
            val steps = 360
            for (i in 0..steps) {
                val angle = -PI / 2 + 2 * PI * i / steps
                val r = radius * (0.84f + 0.16f * abs(sin(PETALS / 2.0 * (angle + PI / 2))).toFloat())
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (i == 0) flower.moveTo(x, y) else flower.lineTo(x, y)
            }
            val measure = PathMeasure().apply { setPath(flower, false) }
            val traced = Path()
            measure.getSegment(0f, measure.length * progress.value, traced, true)
            drawPath(traced, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        content()
    }
}

private const val PETALS = 8
