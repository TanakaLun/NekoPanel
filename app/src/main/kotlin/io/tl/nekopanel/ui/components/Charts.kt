package io.tl.nekopanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
 * A rounded, bordered window frame used to wrap a [TrafficChart]. Its clip keeps the
 * curve contained so the line always fills the box, and the border/background give
 * the chart a layered "viewing window" look.
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
 * A windowed strip chart of an infinitely long signal. The curve is drawn past both
 * edges of the canvas and clipped by the enclosing [ChartWindow], so the line always
 * spans the full box width while the wave translates rigidly and smoothly through it.
 *
 * The buffer samples glide left with a frame-driven offset (values never morph), the
 * live value is drawn at the right edge as the newest sample entering the window, and
 * the vertical scale uses a fixed zero baseline whose top only updates when a sample
 * rolls in, so the waveform isn't deformed frame-by-frame.
 */
@Composable
fun TrafficChart(currentValue: Long, color: Color, modifier: Modifier = Modifier) {
    val capacity = 60
    val sampleIntervalMs = 500f

    val data = remember { mutableStateListOf<Long>().apply { repeat(capacity) { add(0L) } } }
    var offset by remember { mutableFloatStateOf(0f) }
    var yMax by remember { mutableFloatStateOf(1f) }
    val currentValueState by rememberUpdatedState(currentValue)

    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                val dtMs = (frame - lastFrame) / 1_000_000f
                lastFrame = frame
                offset += dtMs / sampleIntervalMs
                if (offset >= 1f) {
                    offset -= 1f
                    data.add(currentValueState)
                    data.removeAt(0)
                    yMax = maxOf((data.maxOrNull() ?: 0L).toFloat(), 1f)
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val n = data.size
        if (n < 2) return@Canvas
        val width = size.width
        val height = size.height
        val cellW = width / n

        // points 0..n-1 are the rolled samples; n and n+1 are the live value, with the
        // extra copy extending past the right edge so the box is always fully covered
        fun getX(index: Int) = index * cellW - offset * cellW
        fun getY(value: Float) = height - (value / yMax).coerceIn(0f, 1f) * height
        fun valueAt(index: Int): Float =
            if (index < n) data[index].toFloat() else currentValueState.toFloat()

        val strokePath = Path().apply {
            moveTo(getX(0), getY(valueAt(0)))
            for (i in 0..n) {
                val x1 = getX(i); val y1 = getY(valueAt(i))
                val x2 = getX(i + 1); val y2 = getY(valueAt(i + 1))
                cubicTo(x1 + (x2 - x1) / 2f, y1, x1 + (x2 - x1) / 2f, y2, x2, y2)
            }
        }
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(getX(n + 1), height)
            lineTo(getX(0), height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(strokePath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
