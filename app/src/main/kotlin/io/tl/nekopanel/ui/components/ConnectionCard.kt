package io.tl.nekopanel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.basicMarquee
import io.tl.nekopanel.model.ConnectionDetail
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.util.formatSize
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConnectionCard(conn: ConnectionItem, onClick: () -> Unit, onClose: () -> Unit) {
    val detail = remember(conn.rawJson) {
        try {
            val json = JSONObject(conn.rawJson)
            val metadata = json.optJSONObject("metadata") ?: JSONObject()

            val host = metadata.optString("host", "")
            val sniff = metadata.optString("sniffHost", "")
            val ip = metadata.optString("destinationIP", "")
            val targetStr = host.ifEmpty { sniff.ifEmpty { ip } }

            val processName = metadata.optString("process", "Unknown")
                .substringAfterLast("/")
                .substringAfterLast("\\")

            val net = metadata.optString("network", "TCP").uppercase()
            val type = metadata.optString("type", "DIRECT").uppercase()

            val chains = json.optJSONArray("chains")
            val node = if (chains != null && chains.length() > 0) chains.optString(0) else "DIRECT"

            val ruleStr = json.optString("rulePayload", json.optString("rule", "Match"))

            val startStr = json.optString("start", "")
            val time = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.Instant.parse(startStr).toEpochMilli()
                } else {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(startStr)?.time ?: System.currentTimeMillis()
                }
            } catch (e: Exception) { System.currentTimeMillis() }

            ConnectionDetail(target = targetStr, process = processName, networkInfo = "$net · $type", routeNode = node, rule = ruleStr, startTimeMillis = time)
        } catch (e: Exception) {
            ConnectionDetail(conn.host, "Error", "TCP", "Unknown", "", System.currentTimeMillis())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = detail.target, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f).basicMarquee())
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = MiuixTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = detail.networkInfo, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = detail.process, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE))
                Spacer(Modifier.width(6.dp))
                Text(text = detail.routeNode, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 2.dp).widthIn(max = 100.dp).basicMarquee(iterations = Int.MAX_VALUE), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrafficStatItem(symbol = "↑", value = conn.upload.formatSize(), color = MiuixTheme.colorScheme.primary)
                    TrafficStatItem(symbol = "↓", value = conn.download.formatSize(), color = MiuixTheme.colorScheme.secondary)
                }
                DurationBadge(detail.startTimeMillis)
            }
        }
    }
}

@Composable
private fun TrafficStatItem(symbol: String, value: String, color: Color) {
    Text(text = "$symbol $value", style = MiuixTheme.textStyles.footnote2, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
}