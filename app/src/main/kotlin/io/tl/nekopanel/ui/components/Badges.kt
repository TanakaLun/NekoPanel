package io.tl.nekopanel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TypeBadge(text: String, style: String, cornerRadius: Int, isFixedSize: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        color = if (style == "填充") color else Color.Transparent,
        shape = RoundedCornerShape(cornerRadius.dp),
        border = if (style == "描边") BorderStroke(1.dp, color) else null,
        modifier = if (isFixedSize) Modifier.width(60.dp).height(20.dp) else Modifier.wrapContentSize()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = if (!isFixedSize) Modifier.padding(horizontal = 6.dp, vertical = 2.dp) else Modifier.fillMaxSize()) {
            Text(
                text.uppercase(),
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                color = if (style == "填充") Color.White else color
            )
        }
    }
}

@Composable
fun DelayBadge(delay: Int, isTesting: Boolean, style: String, cornerRadius: Int, isFixedSize: Boolean, onClick: () -> Unit) {
    val color = when {
        isTesting -> MaterialTheme.colorScheme.primary
        delay <= 0 -> MaterialTheme.colorScheme.outline
        delay < 200 -> Color(0xFF388E3C)
        delay < 500 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }
    val display = if (isTesting) "......" else if (delay <= 0) "TEST" else "${delay}ms"
    Surface(
        color = if (style == "填充") (if (isTesting) color.copy(0.6f) else color) else Color.Transparent,
        shape = RoundedCornerShape(cornerRadius.dp),
        border = if (style == "描边") BorderStroke(1.dp, color.copy(if (isTesting) 0.5f else 1f)) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = if (!isFixedSize) Modifier.padding(horizontal = 6.dp, vertical = 2.dp) else Modifier.fillMaxSize()) {
            Text(
                display,
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                color = if (style == "填充") Color.White else color
            )
        }
    }
}

@Composable
fun DurationBadge(startTimeMillis: Long) {
    val durationText by produceState(initialValue = "...", startTimeMillis) {
        while (true) {
            val diffSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
            value = when {
                diffSeconds < 60 -> "${diffSeconds}s"
                diffSeconds < 3600 -> "${diffSeconds / 60}m"
                else -> "${diffSeconds / 3600}h"
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = CircleShape) {
        Text(text = durationText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}
