package io.tl.nekopanel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.screens.*
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

                if (settings.apiBaseUrl.isBlank()) {
                    InitialSetupDialog(settings) {
                        ApiClient.baseUrl = settings.apiBaseUrl
                        ApiClient.secret = settings.apiSecret
                    }
                } else {
                    ClashManagerApp(settings = settings, onPureBlackToggle = { pureBlackMode = it })
                }
            }
        }
    }
}

// 把这个对话框放在 MainActivity 同级文件或 components 都不合适，就放这里
@Composable
fun InitialSetupDialog(settings: SettingsManager, onConfigured: () -> Unit) {
    var url by remember { mutableStateOf("http://127.0.0.1:9090") }
    var secret by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("欢迎使用 NekoPanel", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                Text("请输入 Mihomo 核心连接信息", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("API 地址") }, singleLine = true)
                OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("密钥 (可选)") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    Button(onClick = {
                        if (url.isNotBlank()) {
                            settings.apiBaseUrl = url.trimEnd('/')
                            settings.apiSecret = secret
                            onConfigured()
                        } else Toast.makeText(context, "地址不能为空", Toast.LENGTH_SHORT).show()
                    }) { Text("连接") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClashManagerApp(settings: SettingsManager, onPureBlackToggle: (Boolean) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var trafficTab by remember { mutableIntStateOf(0) }
    var globalRefreshTick by remember { mutableLongStateOf(0L) }
    var configUpdateTrigger by remember { mutableIntStateOf(0) }

    val logs = remember { mutableStateListOf<LogItem>() }
    var connections by remember { mutableStateOf<List<ConnectionItem>>(emptyList()) }
    var currentMode by remember { mutableStateOf("rule") }

    var globalInUse by remember { mutableLongStateOf(0L) }
    var globalDown by remember { mutableLongStateOf(0L) }
    var globalUp by remember { mutableLongStateOf(0L) }
    var totalDown by remember { mutableLongStateOf(0L) }
    var totalUp by remember { mutableLongStateOf(0L) }
    var currentLogLevel by remember { mutableStateOf(settings.logLevel) }

    val memHistory = rememberChartHistory(globalInUse)
    val downHistory = rememberChartHistory(globalDown)

    // 累计流量逻辑
    val lastRaw = remember { mutableStateOf(settings.getLastRawTraffic()) }
    LaunchedEffect(totalDown, totalUp) {
        settings.updateCumulativeTraffic(totalDown, totalUp, lastRaw.value.first, lastRaw.value.second)
        settings.saveLastRawTraffic(totalDown, totalUp)
        lastRaw.value = settings.getLastRawTraffic()
    }

    // 全局 WebSocket
    LaunchedEffect(settings.backgroundWebSocket, currentLogLevel) {
        if (settings.backgroundWebSocket && settings.apiBaseUrl.isNotBlank()) {
            val logWs = ApiClient.buildWebSocket(
                "/logs?level=$currentLogLevel",
                onText = { text ->
                    try {
                        val obj = JSONObject(text)
                        logs.add(LogItem(obj.optString("type", ""), obj.optString("payload", "")))
                        if (logs.size > 1000) logs.removeAt(0)
                    } catch (_: Exception) {}
                }
            )
            val connWs = ApiClient.buildWebSocket(
                "/connections?interval=1000",
                onText = { text ->
                    try {
                        val arr = JSONObject(text).getJSONArray("connections")
                        val list = mutableListOf<ConnectionItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val meta = obj.getJSONObject("metadata")
                            val chains = obj.getJSONArray("chains")
                            val proxy = if (chains.length() > 0) chains.getString(chains.length() - 1) else "Direct"
                            list.add(
                                ConnectionItem(
                                    id = obj.getString("id"),
                                    host = meta.optString("host").ifBlank { meta.optString("destinationIP") },
                                    network = meta.optString("network"),
                                    proxy = proxy,
                                    upload = obj.optLong("upload", 0L),
                                    download = obj.optLong("download", 0L),
                                    rawJson = obj.toString()
                                )
                            )
                        }
                        connections = list
                    } catch (_: Exception) {}
                }
            )
            val memWs = ApiClient.buildWebSocket(
                "/memory",
                onText = { text ->
                    try {
                        globalInUse = JSONObject(text).optLong("inuse", 0L)
                    } catch (_: Exception) {}
                }
            )
            val trafficWs = ApiClient.buildWebSocket(
                "/traffic",
                onText = { text ->
                    try {
                        val obj = JSONObject(text)
                        globalDown = obj.optLong("down", 0L)
                        globalUp = obj.optLong("up", 0L)
                        totalDown = obj.optLong("downTotal", 0L)
                        totalUp = obj.optLong("upTotal", 0L)
                    } catch (_: Exception) {}
                }
            )
            try { delay(Long.MAX_VALUE) } finally {
                logWs.cancel(); connWs.cancel(); memWs.cancel(); trafficWs.cancel()
            }
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
                0 -> ProxiesScreen(settings, globalRefreshTick, currentMode, onRefresh = { globalRefreshTick = System.currentTimeMillis() }, onModeChange = { configUpdateTrigger++ })
                1 -> RulesScreen(globalRefreshTick, settings)
                2 -> TrafficScreen(trafficTab, logs, connections, settings, currentLogLevel, globalInUse, globalDown, totalDown, totalUp, memHistory, downHistory, onLevelChange = { currentLogLevel = it })
                3 -> FullSettingsScreen(settings, onPureBlackToggle)
            }
        }
    }
}