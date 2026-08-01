package io.tl.nekopanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A rounded, bordered window frame used to wrap a [TrafficChart]. It gives the chart
 * a layered "viewing window" look and clips the curve to the box.
 */
@Composable
fun ChartWindow(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        content = content,
    )
}

/**
 * A fixed, static line chart of the last [capacity] samples. Samples sit at fixed
 * grid positions spanning the full box width (both ends pinned at the edges), joined
 * by straight segments. The line never translates or animates on its own: it only
 * re-renders when a new [currentValue] arrives, so the chart stays smooth, stable and
 * pinned while still reflecting live data.
 *
 * The vertical scale uses a fixed zero baseline; its top grows instantly for new peaks
 * and decays slowly, so spikes leaving the window don't rescale the chart abruptly.
 */
@Composable
fun TrafficChart(currentValue: Long, color: Color, modifier: Modifier = Modifier) {
    val capacity = 60
    val data = remember { mutableStateListOf<Long>().apply { repeat(capacity) { add(0L) } } }
    var yMax by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(currentValue) {
        data.add(currentValue)
        data.removeAt(0)
        yMax = maxOf((data.maxOrNull() ?: 0L).toFloat(), yMax * 0.97f, 1f)
    }

    Canvas(modifier = modifier) {
        val n = data.size
        if (n < 2) return@Canvas
        val width = size.width
        val height = size.height
        val spacing = width / (n - 1)

        fun getX(index: Int) = index * spacing
        fun getY(value: Float) = height - (value / yMax).coerceIn(0f, 1f) * height

        val strokePath = Path().apply {
            moveTo(getX(0), getY(data[0].toFloat()))
            for (i in 0 until n - 1) {
                lineTo(getX(i + 1), getY(data[i + 1].toFloat()))
            }
        }
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(getX(n - 1), height)
            lineTo(getX(0), height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(strokePath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
