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
 * A scrolling strip chart driven by frame time rather than data updates, so it
 * keeps gliding even when traffic is idle and only wiggles when a value arrives.
 *
 * The line scrolls smoothly: the leading value is eased toward the latest sample
 * every frame and blended with the scroll phase (no discrete jumps), and the
 * vertical scale uses a fixed zero baseline with a slowly decaying top so spikes
 * entering/leaving the window don't rescale the whole chart.
 */
@Composable
fun TrafficChart(currentValue: Long, color: Color, modifier: Modifier = Modifier) {
    val capacity = 60
    val sampleIntervalMs = 600f

    val data = remember { mutableStateListOf<Long>().apply { repeat(capacity) { add(0L) } } }
    var phase by remember { mutableFloatStateOf(0f) }
    var displayValue by remember { mutableFloatStateOf(currentValue.toFloat()) }
    var yMax by remember { mutableFloatStateOf(1f) }
    val currentValueState by rememberUpdatedState(currentValue)

    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                val dtMs = (frame - lastFrame) / 1_000_000f
                lastFrame = frame
                // ease the leading value toward the latest sample (≈150ms time constant)
                displayValue += (currentValueState - displayValue) * (dtMs / 150f).coerceAtMost(1f)
                phase += dtMs / sampleIntervalMs
                if (phase >= 1f) {
                    phase -= 1f
                    data.add(displayValue.toLong())
                    data.removeAt(0)
                }
                // keep the top of the scale stable; decay slowly so spikes don't rescale abruptly
                yMax = maxOf((data.maxOrNull() ?: 0L).toFloat(), yMax * (1f - dtMs / 2000f), 1f)
            }
        }
    }

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val columnWidth = width / (data.size - 1)

        fun getX(index: Int) = width - (data.size - 1 - index + phase) * columnWidth
        fun getY(value: Float) = height - (value / yMax).coerceIn(0f, 1f) * height
        // newest point blends from the last sampled value toward the eased target by phase
        fun valueAt(index: Int): Float {
            if (index < data.size - 1) return data[index].toFloat()
            return data[index] + (displayValue - data[index]) * phase
        }

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
