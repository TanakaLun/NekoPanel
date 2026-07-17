package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.model.LogItem
import io.tl.nekopanel.ui.components.LevelSpinner

@Composable
fun LogsView(logs: SnapshotStateList<LogItem>, currentLogLevel: String, onLevelChange: (String) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelSpinner(currentLogLevel) { onLevelChange(it) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { logs.clear() }) { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
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
