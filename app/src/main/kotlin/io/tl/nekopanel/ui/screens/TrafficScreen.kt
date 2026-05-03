package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.SettingsManager
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable
fun TrafficScreen(
    trafficTab: Int,
    logs: SnapshotStateList<LogItem>,
    connections: List<ConnectionItem>,
    settings: SettingsManager,
    currentLogLevel: String,
    memoryInUse: Long,
    trafficDown: Long,
    totalDown: Long,
    totalUp: Long,
    memHistory: List<Long>,
    downHistory: List<Long>,
    onLevelChange: (String) -> Unit,
    onRemoveConnection: (String) -> Unit,
    onClearConnections: () -> Unit
) {
    when (trafficTab) {
        0 -> OverviewView(connections, memoryInUse, trafficDown, totalDown, totalUp, memHistory, downHistory, settings)
        1 -> ConnectionsView(connections, onRemoveConnection, onClearConnections)
        2 -> LogsView(logs, currentLogLevel, onLevelChange)
    }
}

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

@Composable
fun ConnectionsView(
    connections: List<ConnectionItem>,
    onRemoveConnection: (String) -> Unit,
    onClearConnections: () -> Unit
) {
    var selectedJson by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "活跃连接: ${connections.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = {
                    scope.launch {
                        ApiClient.deleteAllConnections()
                        onClearConnections()   // 清空本地列表
                    }
                }) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        items(connections, key = { it.id }) { conn ->
            Box(Modifier.clickable { selectedJson = conn.rawJson }) {
                ConnectionCard(
                    conn = conn,
                    onClose = {
                        scope.launch {
                            ApiClient.deleteConnection(conn.id)
                            onRemoveConnection(conn.id)   // 本地移除
                        }
                    }
                )
            }
        }
    }
    
    // if (selectedJson != null) {
        // Dialog(onDismissRequest = { selectedJson = null }) {
            // Card(
                // shape = RoundedCornerShape(16.dp),
                // modifier = Modifier
                    // .fillMaxWidth()
                    // .fillMaxHeight(0.7f)
            // ) {
                // Column(Modifier.padding(16.dp)) {
                    // Text("元数据明细", fontWeight = FontWeight.Black)
                    // Spacer(Modifier.height(8.dp))

                    // val formatted = remember(selectedJson) {
                        // try {
                            // JSONObject(selectedJson!!).toString(4)
                        // } catch (e: Exception) {
                            // selectedJson ?: ""
                        // }
                    // }
                    // val annotated = remember(formatted) {
                        // highlightJson(formatted)
                    // }

                    // Box(
                        // modifier = Modifier
                            // .weight(1f)
                            // .verticalScroll(rememberScrollState())
                    // ) {
                        // SelectionContainer {
                            // Text(text = annotated, fontSize = 11.sp, lineHeight = 14.sp, fontFamily = FontFamily.Monospace)
                        // }
                    // }
                    // TextButton(
                        // onClick = { selectedJson = null },
                        // modifier = Modifier.align(Alignment.End)
                    // ) {
                        // Text("关闭")
                    // }
                // }
            // }
        // }
    // }
// }
  

    if (selectedJson != null) {
        Dialog(onDismissRequest = { selectedJson = null }) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "元数据明细",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(Modifier.height(12.dp))
    
                    val formatted = remember(selectedJson) {
                        try {
                            JSONObject(selectedJson!!).toString(4)
                        } catch (e: Exception) {
                            selectedJson ?: ""
                        }
                    }
                    
                    val annotated = highlightJson(formatted)
    
                    // Surface(
                        // modifier = Modifier.weight(1f),
                        // // color = MaterialTheme.colorScheme.surfaceContainerLowest, // 更深的衬底
                        // shape = RoundedCornerShape(12.dp),
                        // border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    // ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = annotated,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    // }
    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedJson = null }) {
                            Text("确定")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LogsView(logs: SnapshotStateList<LogItem>, currentLogLevel: String, onLevelChange: (String) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelSpinner(currentLogLevel) { onLevelChange(it) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { logs.clear() }) { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) }
        }
        Surface(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 16.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(logs) { index, log ->
                    Column {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(text = log.type.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.width(60.dp), color = when (log.type.lowercase()) {
                                "info" -> MaterialTheme.colorScheme.primary; "warning" -> Color(0xFFF57C00); "error" -> MaterialTheme.colorScheme.error; "debug" -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.secondary
                            })
                            Text(text = log.payload, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (index < logs.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}