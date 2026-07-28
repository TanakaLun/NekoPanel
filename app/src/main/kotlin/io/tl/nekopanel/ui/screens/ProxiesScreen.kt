package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    var delayCache by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var groupSelections by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isGlobalTesting by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        try {
            allProxies = ApiClient.getProxies()
            val proxiesJson = allProxies ?: return@LaunchedEffect
            val proxiesObj = proxiesJson.getJSONObject("proxies")
            val newDelayCache = mutableMapOf<String, Int>()
            val newSelections = mutableMapOf<String, String>()

            val keys = proxiesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val proxy = proxiesObj.getJSONObject(key)
                val now: String? = proxy.optString("now", null)
                if (now != null) {
                    newSelections[key] = now
                }
                val historyArr = proxy.optJSONArray("history")
                if (historyArr != null && historyArr.length() > 0) {
                    val lastDelay = historyArr.getJSONObject(0).optInt("delay", 0)
                    if (lastDelay > 0) {
                        newDelayCache[key] = lastDelay
                    }
                }
            }

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
            obj.optJSONArray("all") != null && !obj.optBoolean("hidden", false)
        }
        if (settings.showGlobal) filtered else filtered.filter { it != "GLOBAL" }
    }.sorted()

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
                Icon(Icons.Default.Speed, "全面测速", tint = if (isGlobalTesting) MiuixTheme.colorScheme.primary.copy(0.5f) else MiuixTheme.colorScheme.primary)
            }
        }

        val columns = if (settings.groupColumnCount == 1 || isGlobalMode) 1 else 2
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            val minH = maxHeight + 1.dp
            if (columns == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .heightIn(min = minH)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayKeys.forEach { key ->
                        val group = proxiesJson.getJSONObject("proxies").getJSONObject(key)
                        val currentNow = groupSelections[key] ?: group.optString("now", "-")
                        ProxyGroupCard(
                            name = key, group = group, now = currentNow,
                            delayCache = delayCache, settings = settings,
                            onDelayUpdate = updateDelay,
                            onNodeSelected = { node -> selectNode(key, node) },
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .heightIn(min = minH)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayKeys.chunked(2).forEach { chunk ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { key ->
                            val group = proxiesJson.getJSONObject("proxies").getJSONObject(key)
                            val currentNow = groupSelections[key] ?: group.optString("now", "-")
                            Box(Modifier.weight(1f)) {
                                ProxyGroupCard(
                                    name = key, group = group, now = currentNow,
                                    delayCache = delayCache, settings = settings,
                                    onDelayUpdate = updateDelay,
                                    onNodeSelected = { node -> selectNode(key, node) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}}