package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.SettingsManager
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ProxiesScreen(
    settings: SettingsManager,
    refreshTick: Long,
    currentMode: String,
    onRefresh: () -> Unit,
    onModeChange: () -> Unit
) {
    var allProxies by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTick) {
        try { allProxies = ApiClient.getProxies() } catch (_: Exception) {}
        isLoading = false
    }

    if (isLoading || allProxies == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val proxiesJson = allProxies!!
    val isGlobalMode = currentMode.lowercase() == "global"
    val allKeys = proxiesJson.getJSONObject("proxies").keys().asSequence().toList()
    val displayKeys = if (isGlobalMode) allKeys.filter { it == "GLOBAL" } else {
        val filtered = allKeys.filter { key ->
            val obj = proxiesJson.getJSONObject("proxies").getJSONObject(key)
            obj.optJSONArray("all") != null
        }
        if (settings.showGlobal) filtered else filtered.filter { it != "GLOBAL" }
    }.sorted()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeSpinner(currentMode) { newMode ->
                scope.launch {
                    ApiClient.updateConfigs(mapOf("mode" to newMode))
                    onModeChange()
                    onRefresh()
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Speed, "全面测速", tint = MaterialTheme.colorScheme.primary)
            }
        }

        val columns = if (settings.groupColumnCount == 1 || isGlobalMode) 1 else 2
        if (columns == 1) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayKeys.size, key = { displayKeys[it] }) { idx ->
                    val key = displayKeys[idx]
                    val group = proxiesJson.getJSONObject("proxies").getJSONObject(key)
                    ProxyGroupCard(
                        name = key,
                        group = group,
                        allProxies = proxiesJson,  // 传入全局数据
                        settings = settings,
                        onUpdated = onRefresh
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayKeys.size, key = { displayKeys[it] }) { idx ->
                    val key = displayKeys[idx]
                    val group = proxiesJson.getJSONObject("proxies").getJSONObject(key)
                    ProxyGroupCard(
                        name = key,
                        group = group,
                        allProxies = proxiesJson,  // 传入全局数据
                        settings = settings,
                        onUpdated = onRefresh
                    )
                }
            }
        }
    }
}