package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.model.LogItem

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
