package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradeCalculator
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradeConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradePoint
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.Rank
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

const val ProgressChartTag = "progressChart"

/** Grade value at which rank [level] starts. */
private fun levelThreshold(level: Int): Double = level * GradeConfig.MAX_VALUE / GradeConfig.LEVEL_COUNT

/**
 * Curve of the grade after each quiz, drawn on a Compose [Canvas] with theme colors only. Rank thresholds are light
 * horizontal lines with their name, the current rank's line is stronger, and the latest point is highlighted.
 * Exposes [ProgressChartTag] and a description with the number of points and the current rank.
 */
@Composable
fun ProgressChart(points: List<GradePoint>, currentRank: Rank, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val currentStyle = labelStyle.copy(color = colors.onSurface, fontWeight = FontWeight.Bold)
    val names = List(GradeConfig.LEVEL_COUNT) { Rank(it).label() }
    val layouts = remember(names, currentRank.level, labelStyle, currentStyle) {
        names.mapIndexed { level, name ->
            measurer.measure(name, if (level == currentRank.level) currentStyle else labelStyle)
        }
    }
    val description = pluralStringResource(R.plurals.stats_chart_description, points.size, points.size, Rank(currentRank.level).label())

    Canvas(modifier = modifier.testTag(ProgressChartTag).semantics { contentDescription = description }) {
        if (points.isEmpty()) return@Canvas
        val labelWidth = layouts.maxOf { it.size.width }
        val left = labelWidth + 10.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = size.height - 14.dp.toPx()

        // Visible rank range: around the data, at least 3 steps tall.
        var lowLevel = GradeCalculator.level(points.minOf { it.value })
        var highLevel = min(GradeConfig.LEVEL_COUNT - 1, GradeCalculator.level(points.maxOf { it.value }) + 1)
        while (highLevel - lowLevel < 3) {
            if (highLevel < GradeConfig.LEVEL_COUNT - 1) highLevel++ else if (lowLevel > 0) lowLevel-- else break
        }
        val yMin = levelThreshold(lowLevel)
        val yMax = max(levelThreshold(highLevel), points.maxOf { it.value })
        val span = (yMax - yMin).coerceAtLeast(1e-6)
        fun yOf(value: Double) = bottom - ((value - yMin) / span).toFloat() * (bottom - top)
        fun xOf(index: Int) = if (points.size == 1) (left + right) / 2 else left + (right - left) * index / (points.size - 1)

        // Rank thresholds: a label only every few lines when the range is tall.
        val rowHeight = (bottom - top) * (levelThreshold(1) / span).toFloat()
        val stride = max(1, ceil(18.dp.toPx() / rowHeight).toInt())
        for (level in lowLevel..highLevel) {
            val y = yOf(levelThreshold(level))
            val isCurrent = level == currentRank.level
            drawLine(
                color = if (isCurrent) colors.outline else colors.outlineVariant,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = if (isCurrent) 1.5.dp.toPx() else 1.dp.toPx(),
            )
            if (isCurrent || (level - lowLevel) % stride == 0) {
                val layout = layouts[level]
                drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
            }
        }

        // Curve: horizontal tangents at each point, so it never overshoots between two quizzes.
        val coords = points.mapIndexed { i, p -> Offset(xOf(i), yOf(p.value)) }
        val curve = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val a = coords[i - 1]
                val b = coords[i]
                val mid = (a.x + b.x) / 2
                cubicTo(mid, a.y, mid, b.y, b.x, b.y)
            }
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(coords.last().x, bottom)
            lineTo(coords.first().x, bottom)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.28f), colors.primary.copy(alpha = 0f)), startY = top, endY = bottom))
        drawPath(curve, colors.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        if (coords.size <= 30) coords.dropLast(1).forEach { drawCircle(colors.primary, 3.dp.toPx(), it) }
        val last = coords.last()
        drawCircle(colors.primaryContainer, 13.dp.toPx(), last)
        drawCircle(colors.surface, 8.dp.toPx(), last)
        drawCircle(colors.primary, 5.5.dp.toPx(), last)
    }
}
