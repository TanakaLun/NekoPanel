package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
    trafficUp: Long,
    trafficDown: Long,
    totalDown: Long,
    totalUp: Long,
    scaffoldPadding: PaddingValues = PaddingValues(),
    onLevelChange: (String) -> Unit,
    onRemoveConnection: (String) -> Unit,
    onClearConnections: () -> Unit
) {
    when (trafficTab) {
        0 -> OverviewView(connections, memoryInUse, trafficUp, trafficDown, totalDown, totalUp, settings, scaffoldPadding)
        1 -> ConnectionsView(connections, onRemoveConnection, onClearConnections, scaffoldPadding)
        2 -> LogsView(logs, currentLogLevel, onLevelChange, scaffoldPadding)
    }
}
