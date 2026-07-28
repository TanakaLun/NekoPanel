package io.tl.nekopanel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.model.LogItem
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.service.DataDaemonService
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.screens.*
import io.tl.nekopanel.ui.theme.AllThemeSchemes
import io.tl.nekopanel.ui.theme.NekoPanelTheme
import kotlinx.coroutines.*
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val settings = SettingsManager(this)

        setContent {
            NekoPanelApp(settings = settings)
        }
    }

    companion object {
        fun requestBatteryExemption(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun NekoPanelApp(settings: SettingsManager) {
    var themeModeState by remember { mutableStateOf(settings.themeMode) }
    var dynColorState by remember { mutableStateOf(settings.dynamicColorEnabled) }
    var customColorKey by remember { mutableStateOf(settings.customThemeColorKey) }
    var isConfigured by remember { mutableStateOf(settings.apiBaseUrl.isNotBlank()) }
    val customSeedColor = remember(customColorKey) {
        AllThemeSchemes.firstOrNull { it.key == customColorKey }?.seedColor
    }

    NekoPanelTheme(
        themeMode = themeModeState,
        dynamicColor = dynColorState,
        customSeedColor = customSeedColor,
    ) {
        ApiClient.baseUrl = settings.apiBaseUrl
        ApiClient.secret = settings.apiSecret

        if (!isConfigured) {
            InitialSetupPage(settings) {
                isConfigured = true
                ApiClient.baseUrl = settings.apiBaseUrl
                ApiClient.secret = settings.apiSecret
            }
        } else {
            ClashManagerApp(
                settings = settings,
                onThemeModeChange = { themeModeState = it; settings.themeMode = it },
                onDynamicColorChange = { dynColorState = it; settings.dynamicColorEnabled = it },
                onCustomColorChange = { customColorKey = it; settings.customThemeColorKey = it },
            )
        }
    }
}

@Composable
fun InitialSetupPage(settings: SettingsManager, onConfigured: () -> Unit) {
    var url by remember { mutableStateOf("http://127.0.0.1:9090") }
    var secret by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("欢迎使用 NekoPanel", fontWeight = FontWeight.Black, style = MiuixTheme.textStyles.title2)
                Text("请配置 Clash API 地址以开始使用", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                TextField(value = url, onValueChange = { url = it }, label = "API 地址", singleLine = true, modifier = Modifier.fillMaxWidth())
                TextField(value = secret, onValueChange = { secret = it }, label = "密钥 (可选)", singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (url.isNotBlank()) {
                        settings.apiBaseUrl = url.trimEnd('/')
                        settings.apiSecret = secret
                        onConfigured()
                    } else Toast.makeText(context, "地址不能为空", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("连接") }
            }
        }
    }
}

enum class Page { MAIN, UI_SETTINGS, BACKUP }

@Composable
fun ClashManagerApp(settings: SettingsManager, onThemeModeChange: (String) -> Unit = {}, onDynamicColorChange: (Boolean) -> Unit = {}, onCustomColorChange: (String) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var trafficTab by remember { mutableIntStateOf(0) }
    var globalRefreshTick by remember { mutableLongStateOf(0L) }
    var configUpdateTrigger by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableStateOf(Page.MAIN) }
    var backAnimStyle by remember { mutableStateOf(settings.backAnimStyle) }
    val context = LocalContext.current

    val logs = remember { mutableStateListOf<LogItem>() }
    var connections by remember { mutableStateOf<List<ConnectionItem>>(emptyList()) }
    var currentMode by remember { mutableStateOf("rule") }

    LaunchedEffect(Unit) {
        try {
            val cfg = ApiClient.getConfigs()
            val mode = cfg.optString("mode", "rule")
            if (mode.isNotBlank()) currentMode = mode
        } catch (_: Exception) {}
    }

    var globalInUse by remember { mutableLongStateOf(0L) }
    var globalDown by remember { mutableLongStateOf(0L) }
    var globalUp by remember { mutableLongStateOf(0L) }
    var totalDown by remember { mutableLongStateOf(0L) }
    var totalUp by remember { mutableLongStateOf(0L) }
    var currentLogLevel by remember { mutableStateOf(settings.logLevel) }

    val memHistory = rememberChartHistory(globalInUse)
    val downHistory = rememberChartHistory(globalDown)

    LaunchedEffect(Unit) {
        if (settings.backgroundWebSocket || settings.autoStartService) {
            DataDaemonService.start(context)
        }
    }

    val removeConnection: (String) -> Unit = { id ->
        connections = connections.filter { it.id != id }
    }
    val clearConnections: () -> Unit = {
        connections = emptyList()
    }

    LaunchedEffect(Unit) {
        if (settings.apiBaseUrl.isBlank()) return@LaunchedEffect

        launch {
            while (isActive) {
                val fail = CompletableDeferred<Unit>()
                val ws = ApiClient.buildWebSocket("/memory", onText = { text ->
                    try { globalInUse = JSONObject(text).optLong("inuse", 0L) } catch (_: Exception) {}
                }, onError = { fail.complete(Unit) })
                try { fail.await() } catch (_: CancellationException) { ws.cancel(); break } finally { ws.cancel() }
                delay(3000)
            }
        }
        launch {
            while (isActive) {
                val fail = CompletableDeferred<Unit>()
                val ws = ApiClient.buildWebSocket("/traffic", onText = { text ->
                    try {
                        val obj = JSONObject(text)
                        val d = obj.optLong("down", 0L)
                        val u = obj.optLong("up", 0L)
                        val dt = obj.optLong("downTotal", 0L)
                        val ut = obj.optLong("upTotal", 0L)
                        val dc = obj.optLong("downCumulative", -1L)
                        val uc = obj.optLong("upCumulative", -1L)
                        globalDown = d; globalUp = u; totalDown = dt; totalUp = ut
                        settings.setTrafficSnapshot(d, u, dt, ut, dc, uc)
                    } catch (_: Exception) {}
                }, onError = { fail.complete(Unit) })
                try { fail.await() } catch (_: CancellationException) { ws.cancel(); break } finally { ws.cancel() }
                delay(3000)
            }
        }
        launch {
            while (isActive) {
                val fail = CompletableDeferred<Unit>()
                val ws = ApiClient.buildWebSocket("/logs?level=$currentLogLevel", onText = { text ->
                    try {
                        val obj = JSONObject(text)
                        logs.add(LogItem(obj.optString("type", ""), obj.optString("payload", "")))
                        if (logs.size > 1000) logs.removeAt(0)
                    } catch (_: Exception) {}
                }, onError = { fail.complete(Unit) })
                try { fail.await() } catch (_: CancellationException) { ws.cancel(); break } finally { ws.cancel() }
                delay(3000)
            }
        }

        delay(Long.MAX_VALUE)
    }

    LaunchedEffect(selectedTab) {
        if (settings.apiBaseUrl.isNotBlank() && selectedTab == 2) {
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
                            list.add(ConnectionItem(
                                id = obj.getString("id"),
                                host = meta.optString("host").ifBlank { meta.optString("destinationIP") },
                                network = meta.optString("network"),
                                proxy = proxy,
                                upload = obj.optLong("upload", 0L),
                                download = obj.optLong("download", 0L),
                                rawJson = obj.toString()
                            ))
                        }
                        connections = list
                    } catch (_: Exception) {}
                }
            )
            try { delay(Long.MAX_VALUE) } finally {
                connWs.cancel()
            }
        }
    }

    BackHandler(currentPage == Page.UI_SETTINGS || currentPage == Page.BACKUP) {
        currentPage = Page.MAIN
    }

    BackHandler(currentPage == Page.MAIN) {
        (context as? Activity)?.moveTaskToBack(true)
    }
    BackHandler(currentPage == Page.MAIN) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val transitionSpec = when (backAnimStyle) {
        "scale" -> fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)) togetherWith
            fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300))
        "none" -> fadeIn() togetherWith fadeOut()
        else -> slideInHorizontally(animationSpec = tween(300)) { it / 4 } togetherWith
            slideOutHorizontally(animationSpec = tween(300)) { it / 4 }
    }

    AnimatedContent(targetState = currentPage, transitionSpec = { transitionSpec }, label = "page") { page ->
        when (page) {
            Page.MAIN -> Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    SmallTopAppBar(
                        title = when (selectedTab) { 0 -> "代理"; 1 -> "规则"; 3 -> "设置"; else -> "" },
                        bottomContent = {
                            if (selectedTab == 2) {
                                Box(Modifier.padding(horizontal = 12.dp)) {
                                    TabRow(
                                        tabs = listOf("概览", "连接", "日志"),
                                        selectedTabIndex = trafficTab,
                                        onTabSelected = { trafficTab = it },
                                    )
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar {
                        listOf("代理" to Icons.AutoMirrored.Filled.List, "规则" to Icons.Default.CheckCircle, "监控" to Icons.Default.SwapCalls, "设置" to Icons.Default.Settings).forEachIndexed { index, (label, icon) ->
                            NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index }, icon = icon, label = label)
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (selectedTab) {
                        0 -> ProxiesScreen(settings, globalRefreshTick, currentMode, onRefresh = { globalRefreshTick = System.currentTimeMillis() }, onModeChange = { newMode -> currentMode = newMode; configUpdateTrigger++ })
                        1 -> RulesScreen(globalRefreshTick, settings)
                        2 -> TrafficScreen(trafficTab, logs, connections, settings, currentLogLevel, globalInUse, globalDown, totalDown, totalUp, memHistory, downHistory, onLevelChange = { currentLogLevel = it; settings.logLevel = it }, onRemoveConnection = removeConnection, onClearConnections = clearConnections)
                        3 -> FullSettingsScreen(settings, onNavigateToUiSettings = { currentPage = Page.UI_SETTINGS }, onNavigateToBackup = { currentPage = Page.BACKUP })
                    }
                }
            }
            Page.UI_SETTINGS -> Surface(Modifier.fillMaxSize()) {
                UiSettingsScreen(
                    settings,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onCustomColorChange = onCustomColorChange,
                    onBackAnimChange = { backAnimStyle = it; settings.backAnimStyle = it },
                    onBack = { currentPage = Page.MAIN }
                )
            }
            Page.BACKUP -> Surface(Modifier.fillMaxSize()) {
                BackupScreen(settings, onBack = { currentPage = Page.MAIN })
            }
        }
    }
}