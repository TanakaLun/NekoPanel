package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.ConnectionCard
import io.tl.nekopanel.ui.components.highlightJson
import kotlinx.coroutines.launch
import org.json.JSONObject

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
                        onClearConnections()
                    }
                }) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        items(connections, key = { it.id }) { conn ->
            ConnectionCard(
                conn = conn,
                onClick = { selectedJson = conn.rawJson },
                onClose = {
                    scope.launch {
                        ApiClient.deleteConnection(conn.id)
                        onRemoveConnection(conn.id)
                    }
                }
            )
        }
    }
    
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
                }
            }
        }
    }
}
