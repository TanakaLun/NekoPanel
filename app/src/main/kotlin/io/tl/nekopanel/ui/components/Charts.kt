package io.tl.nekopanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A fixed-window strip chart: the line always spans the full width with both ends
 * pinned at the canvas edges. A continuous scroll phase interpolates each column's
 * value between the current sample and the next one, so the wave glides left
 * smoothly instead of jumping by whole columns when a sample rolls in. New data
 * enters at the right edge and travels across the window like viewing an infinite
 * signal through a finite viewport.
 *
 * The vertical scale is a fixed zero baseline with a slowly eased top so spikes
 * entering/leaving the window don't rescale the whole chart abruptly.
 */
@Composable
fun TrafficChart(currentValue: Long, color: Color, modifier: Modifier = Modifier) {
    val capacity = 60
    val sampleIntervalMs = 500f

    val data = remember { mutableStateListOf<Long>().apply { repeat(capacity) { add(0L) } } }
    var phase by remember { mutableFloatStateOf(0f) }
    var yMax by remember { mutableFloatStateOf(1f) }
    val currentValueState by rememberUpdatedState(currentValue)

    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                val dtMs = (frame - lastFrame) / 1_000_000f
                lastFrame = frame
                phase += dtMs / sampleIntervalMs
                if (phase >= 1f) {
                    phase -= 1f
                    data.add(currentValueState)
                    data.removeAt(0)
                }
                // ease the top of the scale toward the data max so rescaling is smooth
                val dataMax = (data.maxOrNull() ?: 0L).toFloat()
                yMax += (maxOf(dataMax, 1f) - yMax) * (dtMs / 400f).coerceAtMost(1f)
            }
        }
    }

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)

        fun getX(index: Int) = index * spacing
        // continuous wave: each column morphs from its sample toward the next one by phase,
        // so the whole pattern glides left; the rightmost column morphs toward the newest value
        fun valueAt(index: Int): Float {
            if (index < data.size - 1) {
                return data[index] + (data[index + 1] - data[index]) * phase
            }
            return data[index] + (currentValueState - data[index]) * phase
        }
        fun getY(value: Float) = height - (value / yMax).coerceIn(0f, 1f) * height

        val strokePath = Path().apply {
            moveTo(getX(0), getY(valueAt(0)))
            for (i in 0 until data.size - 1) {
                val x1 = getX(i); val y1 = getY(valueAt(i))
                val x2 = getX(i + 1); val y2 = getY(valueAt(i + 1))
                cubicTo(x1 + (x2 - x1) / 2f, y1, x1 + (x2 - x1) / 2f, y2, x2, y2)
            }
        }
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(getX(data.size - 1), height)
            lineTo(getX(0), height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(strokePath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
