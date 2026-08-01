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
 * A windowed strip chart that shows an infinitely long signal through a fixed
 * viewport: the wave translates rigidly to the left (sample values never morph,
 * only their x positions glide), so the shape stays stable and moves smoothly.
 * The live value is drawn at the right edge as the newest sample entering the
 * window, and older samples roll out on the left.
 *
 * The vertical scale uses a fixed zero baseline; its top is recomputed only when
 * a sample rolls in, so the waveform is not deformed frame-by-frame.
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
        val spacing = width / n

        // index 0..n-1 are the rolled samples, index n is the live value at the right edge
        fun getX(index: Int) = width - (n - index + phase) * spacing
        fun getY(value: Float) = height - (value / yMax).coerceIn(0f, 1f) * height
        fun valueAt(index: Int): Float =
            if (index < n) data[index].toFloat() else currentValueState.toFloat()

        val strokePath = Path().apply {
            moveTo(getX(0), getY(valueAt(0)))
            for (i in 0 until n) {
                val x1 = getX(i); val y1 = getY(valueAt(i))
                val x2 = getX(i + 1); val y2 = getY(valueAt(i + 1))
                cubicTo(x1 + (x2 - x1) / 2f, y1, x1 + (x2 - x1) / 2f, y2, x2, y2)
            }
        }
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(getX(n), height)
            lineTo(getX(0), height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(strokePath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
