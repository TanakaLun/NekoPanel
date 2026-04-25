package io.tl.nekopanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.screens.*
import io.tl.nekopanel.ui.theme.ComposeEmptyActivityTheme
import kotlinx.coroutines.*
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsManager(this)
        setContent {
            var pureBlackMode by remember { mutableStateOf(settings.pureBlackMode) }
            ComposeEmptyActivityTheme(pureBlackMode = pureBlackMode) {
                ApiClient.baseUrl = settings.apiBaseUrl
                ApiClient.secret = settings.apiSecret
                ClashManagerApp(settings) { pureBlackMode = it }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClashManagerApp(settings: SettingsManager, onPureBlackToggle: (Boolean) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var trafficTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<LogItem>() }
    var connections by remember { mutableStateOf<List<ConnectionItem>>(emptyList()) }
    var currentLogLevel by remember { mutableStateOf(settings.logLevel) }
    var globalInUse by remember { mutableLongStateOf(0L) }
    var globalDown by remember { mutableLongStateOf(0L) }
    var totalDown by remember { mutableLongStateOf(0L) }
    var totalUp by remember { mutableLongStateOf(0L) }
    val memHistory = rememberChartHistory(globalInUse)
    val downHistory = rememberChartHistory(globalDown)
    var globalRefreshTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(currentLogLevel) {
        val logWs = ApiClient.buildWebSocket("/logs?level=$currentLogLevel", onText = { text ->
            val obj = JSONObject(text)
            logs.add(0, LogItem(obj.optString("type"), obj.optString("payload")))
            if (logs.size > 500) logs.removeAt(logs.size - 1)
        })
        val trafficWs = ApiClient.buildWebSocket("/traffic", onText = { text ->
            val obj = JSONObject(text)
            globalDown = obj.optLong("down")
            totalDown = obj.optLong("downTotal")
            totalUp = obj.optLong("upTotal")
        })
        val connWs = ApiClient.buildWebSocket("/connections", onText = { text ->
            val obj = JSONObject(text)
            globalInUse = obj.optLong("memory")
            val arr = obj.optJSONArray("connections") ?: return@buildWebSocket
            val list = mutableListOf<ConnectionItem>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val metadata = c.optJSONObject("metadata") ?: JSONObject()
                val host = metadata.optString("host").let { if(it.isEmpty()) metadata.optString("destinationIP") else it }
                
                list.add(ConnectionItem(
                    id = c.optString("id"),
                    host = host,
                    network = metadata.optString("network"),
                    proxy = c.optString("chains").let { 
                        val jsonArr = JSONObject(text).optJSONArray("connections")?.getJSONObject(i)?.optJSONArray("chains")
                        jsonArr?.optString(0) ?: "DIRECT"
                    },
                    upload = c.optLong("upload"),
                    download = c.optLong("download"),
                    start = c.optString("start"),
                    chains = mutableListOf<String>().apply { 
                        val ch = c.optJSONArray("chains")
                        if(ch != null) for(j in 0 until ch.length()) add(ch.getString(j))
                    },
                    rule = c.optString("rule"),
                    rulePayload = c.optString("rulePayload"),
                    rawJson = c.toString()
                ))
            }
            connections = list
        })
        try { awaitCancellation() } finally {
            logWs.close(1000, null); trafficWs.close(1000, null); connWs.close(1000, null)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                if (selectedTab == 2) CapsuleTabRow(trafficTab, { trafficTab = it }, listOf("概览", "连接", "日志"))
                else Text(when (selectedTab) { 0 -> "代理"; 1 -> "规则"; 3 -> "设置"; else -> "NekoPanel" }, fontWeight = FontWeight.Black)
            })
        },
        bottomBar = {
            NavigationBar {
                listOf("代理" to Icons.AutoMirrored.Filled.List, "规则" to Icons.Default.CheckCircle, "监控" to Icons.Default.SwapCalls, "设置" to Icons.Default.Settings).forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ProxiesScreen(settings, globalRefreshTick, "rule", onRefresh = { globalRefreshTick = System.currentTimeMillis() }, onModeChange = {})
                1 -> RulesScreen(globalRefreshTick, settings)
                2 -> TrafficScreen(trafficTab, logs, connections, settings, currentLogLevel, globalInUse, globalDown, totalDown, totalUp, memHistory, downHistory, onLevelChange = { currentLogLevel = it })
                3 -> FullSettingsScreen(settings, onPureBlackToggle)
            }
        }
    }
}
