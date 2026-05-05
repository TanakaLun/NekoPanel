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
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ProxiesScreen(
    settings: SettingsManager,
    refreshTick: Long,
    currentMode: String,
    onRefresh: () -> Unit,
    onModeChange: (String) -> Unit
) {
    var allProxies by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // 延迟缓存：节点名 -> 延迟(ms)
    var delayCache by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // 选中节点缓存：组名 -> 当前选中的节点名
    var groupSelections by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isGlobalTesting by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        try {
            allProxies = ApiClient.getProxies()
            // 初始化延迟缓存和选中状态
            val proxiesJson = allProxies ?: return@LaunchedEffect
            val proxiesObj = proxiesJson.getJSONObject("proxies")
            val newDelayCache = mutableMapOf<String, Int>()
            val newSelections = mutableMapOf<String, String>()

            val keys = proxiesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val proxy = proxiesObj.getJSONObject(key)
                val now = proxy.optString("now", null)
                if (now != null) {
                    newSelections[key] = now
                }
                // 从历史记录中提取延迟（如果有的话）
                val historyArr = proxy.optJSONArray("history")
                if (historyArr != null && historyArr.length() > 0) {
                    val lastDelay = historyArr.getJSONObject(0).optInt("delay", 0)
                    if (lastDelay > 0) {
                        newDelayCache[key] = lastDelay   // 注意：历史记录可能只有组本身的延迟（不是节点），需要处理所有节点的延迟
                    }
                }
            }

            // 更准确的方式：遍历所有代理项，包括节点
            val allKeysForDelay = proxiesObj.keys().asSequence().toList()
            for (proxyName in allKeysForDelay) {
                val proxy = proxiesObj.getJSONObject(proxyName)
                val historyArr = proxy.optJSONArray("history")
                if (historyArr != null && historyArr.length() > 0) {
                    val lastDelay = historyArr.getJSONObject(0).optInt("delay", 0)
                    if (lastDelay > 0) {
                        newDelayCache[proxyName] = lastDelay
                    }
                }
            }

            delayCache = newDelayCache
            groupSelections = newSelections
        } catch (_: Exception) {}
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

    // 更新延迟缓存或选中节点的回调
    val updateDelay: (String, Int) -> Unit = { node, delay ->
        delayCache = delayCache.toMutableMap().apply { put(node, delay) }
    }
    val selectNode: (String, String) -> Unit = { groupName, nodeName ->
        groupSelections = groupSelections.toMutableMap().apply { put(groupName, nodeName) }
        scope.launch {
            ApiClient.updateProxy(groupName, mapOf("name" to nodeName))
        }
    }

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
                    onModeChange(newMode)
                    onRefresh()
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    isGlobalTesting = true
                    try {
                        val proxiesObj = proxiesJson.getJSONObject("proxies")
                        val groupNames = proxiesObj.keys().asSequence().filter { key ->
                            proxiesObj.getJSONObject(key).optJSONArray("all") != null
                        }.toList()
                        for (groupName in groupNames) {
                            try {
                                val delays = ApiClient.getGroupDelay(groupName, settings.testUrl, settings.testTimeout)
                                val keys = delays.keys()
                                while (keys.hasNext()) {
                                    val node = keys.next()
                                    delayCache = delayCache.toMutableMap().apply { put(node, delays.getInt(node)) }
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {} finally {
                        isGlobalTesting = false
                    }
                }
            }) {
                Icon(Icons.Default.Speed, "全面测速", tint = if (isGlobalTesting) MaterialTheme.colorScheme.primary.copy(0.5f) else MaterialTheme.colorScheme.primary)
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
                    val currentNow = groupSelections[key] ?: group.optString("now", "-")
                    ProxyGroupCard(
                        name = key,
                        group = group,
                        now = currentNow,
                        delayCache = delayCache,
                        settings = settings,
                        onDelayUpdate = updateDelay,
                        onNodeSelected = { node -> selectNode(key, node) },
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
                    val currentNow = groupSelections[key] ?: group.optString("now", "-")
                    ProxyGroupCard(
                        name = key,
                        group = group,
                        now = currentNow,
                        delayCache = delayCache,
                        settings = settings,
                        onDelayUpdate = updateDelay,
                        onNodeSelected = { node -> selectNode(key, node) },
                    )
                }
            }
        }
    }
}