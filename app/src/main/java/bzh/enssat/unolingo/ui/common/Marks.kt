package bzh.enssat.unolingo.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Japanese correction marks: ◯ (maru) for a right answer, ✕ (batsu) for a wrong one. Colors are always doubled by
 * these shapes so right and wrong never rely on color alone. With [animate], the ◯ traces itself with a small
 * bounce (200 ms).
 */
@Composable
fun AnswerMark(correct: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, animate: Boolean = true) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(correct) {
        if (!animate) return@LaunchedEffect
        launch { progress.animateTo(1f, tween(durationMillis = 200, easing = FastOutSlowInEasing)) }
        if (correct) {
            bounce.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 200
                    1f at 120
                    1.04f at 160
                },
            )
        }
    }
    Canvas(
        modifier
            .size(size)
            .scale(bounce.value),
    ) {
        val strokeWidth = this.size.minDimension * 0.16f
        if (correct) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = this.size.copy(this.size.width - strokeWidth, this.size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        } else {
            val inset = strokeWidth
            val end = this.size.width - inset
            val first = (progress.value * 2).coerceAtMost(1f)
            val second = (progress.value * 2 - 1).coerceIn(0f, 1f)
            drawLine(color, Offset(inset, inset), Offset(inset + (end - inset) * first, inset + (end - inset) * first), strokeWidth, StrokeCap.Round)
            if (second > 0f) {
                drawLine(color, Offset(end, inset), Offset(end - (end - inset) * second, inset + (end - inset) * second), strokeWidth, StrokeCap.Round)
            }
        }
    }
}
