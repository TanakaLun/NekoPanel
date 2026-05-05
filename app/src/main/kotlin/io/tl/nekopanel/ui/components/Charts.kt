package io.tl.nekopanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MiniLineChart(data: List<Long>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val maxVal = data.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val minVal = data.minOrNull() ?: 0L
        val range = (maxVal - minVal).coerceAtLeast(1L)
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)

        fun getX(index: Int) = index * spacing
        fun getY(value: Long) = height - ((value - minVal).toFloat() / range * height)

        val strokePath = Path().apply {
            moveTo(getX(0), getY(data[0]))
            for (i in 0 until data.size - 1) {
                val x1 = getX(i); val y1 = getY(data[i])
                val x2 = getX(i + 1); val y2 = getY(data[i + 1])
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

@Composable
fun rememberChartHistory(currentValue: Long): List<Long> {
    val history = remember { mutableStateListOf<Long>() }
    LaunchedEffect(currentValue) {
        history.add(currentValue)
        if (history.size > 50) history.removeAt(0)
    }
    return history
}
