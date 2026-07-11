package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.MiniLineChart
import io.tl.nekopanel.ui.components.TypeBadge
import io.tl.nekopanel.util.formatSize
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun OverviewView(
    connections: List<ConnectionItem>,
    memoryInUse: Long,
    trafficDown: Long,
    totalDown: Long,
    totalUp: Long,
    memHistory: List<Long>,
    downHistory: List<Long>,
    settings: SettingsManager
) {
    val cumulative = remember { settings.getCumulativeTraffic() }
    val topRules = remember { mutableStateListOf<JSONObject>() }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val rules = ApiClient.getRules().getJSONArray("rules")
                val filtered = mutableListOf<JSONObject>()
                for (i in 0 until rules.length()) {
                    val r = rules.getJSONObject(i)
                    if ((r.optJSONObject("extra")?.optInt("hitCount", 0) ?: 0) > 0) filtered.add(r)
                }
                topRules.clear()
                topRules.addAll(filtered.sortedByDescending { it.optJSONObject("extra")?.optInt("hitCount", 0) ?: 0 }.take(5))
            } catch (_: Exception) {}
            delay(5000)
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("系统概览", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("内存占用", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(memoryInUse.formatSize(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            MiniLineChart(memHistory, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("下载速度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${trafficDown.formatSize()}/s", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            MiniLineChart(downHistory, MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp))
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text("总下载", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(totalDown.formatSize(),fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("总上传", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(totalUp.formatSize(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("累计流量 (自记录起)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("↓ ${cumulative.first.formatSize()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("↑ ${cumulative.second.formatSize()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                listOf("活跃连接" to "${connections.size}", "规则命中" to "${topRules.sumOf { it.optJSONObject("extra")?.optInt("hitCount", 0) ?: 0 }}").forEach { (label, value) ->
                    Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item { Text("高频规则命中", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
        items(topRules) { rule ->
            val type = rule.optString("type", "")
            val payload = rule.optString("payload", "")
            val proxy = rule.optString("proxy", "")
            val hitCount = rule.optJSONObject("extra")?.optInt("hitCount", 0) ?: 0
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TypeBadge(type, settings.ruleBadgeStyle, settings.badgeCornerRadius, false)
                            Spacer(Modifier.width(8.dp))
                            Text(payload, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, lineHeight = 18.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("➔ $proxy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                        Text("$hitCount", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
