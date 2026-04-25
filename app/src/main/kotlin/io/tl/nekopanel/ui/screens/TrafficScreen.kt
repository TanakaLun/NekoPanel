package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.ConnectionItem
import io.tl.nekopanel.LogItem
import io.tl.nekopanel.SettingsManager
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.launch

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
    onLevelChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    when (trafficTab) {
        0 -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            TrafficOverviewCard(trafficDown, totalDown, totalUp, downHistory, settings)
            Spacer(Modifier.height(16.dp))
            MemoryCard(memoryInUse, memHistory, settings)
            Spacer(Modifier.height(16.dp))
            ConnectionStatusCard(connections.size)
        }
        1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(connections, key = { it.id }) { conn -> 
                ConnectionCard(
                    conn = conn, 
                    onClose = { scope.launch { try { ApiClient.deleteConnection(conn.id) } catch(_:Exception){} } }
                ) 
            }
        }
        2 -> LogsView(logs, currentLogLevel, onLevelChange)
    }
}

@Composable
fun LogsView(logs: SnapshotStateList<LogItem>, currentLogLevel: String, onLevelChange: (String) -> Unit) {
    val state = rememberLazyListState()
    Column(Modifier.fillMaxSize()) {
        LogLevelSelector(currentLogLevel, onLevelChange)
        LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            itemsIndexed(logs) { index: Int, log: LogItem ->
                Column {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            text = log.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.width(60.dp),
                            color = when (log.type.lowercase()) {
                                "info" -> MaterialTheme.colorScheme.primary
                                "error" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        Text(text = log.payload, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                    }
                    if (index < logs.size - 1) HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
