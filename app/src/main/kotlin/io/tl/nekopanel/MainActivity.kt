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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.navigation.AppState
import io.tl.nekopanel.navigation.LocalAppState
import io.tl.nekopanel.navigation.LocalNavigator
import io.tl.nekopanel.navigation.LocalUpdateAppState
import io.tl.nekopanel.navigation.Navigator
import io.tl.nekopanel.navigation.Route
import io.tl.nekopanel.navigation.WebSocketState
import io.tl.nekopanel.navigation.rememberWebSocketState
import io.tl.nekopanel.navigation.CrossActivityTransition
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.service.DataDaemonService
import io.tl.nekopanel.ui.rememberBlurBackdrop
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.screens.*
import io.tl.nekopanel.ui.theme.AllThemeSchemes
import io.tl.nekopanel.ui.theme.NekoPanelTheme
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
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
        setContent { NekoPanelApp(settings = settings) }
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

    val effectiveSeedColor = remember(customColorKey, dynColorState) {
        if (dynColorState) null
        else AllThemeSchemes.firstOrNull { it.key == customColorKey }?.seedColor
    }

    NekoPanelTheme(
        themeMode = themeModeState,
        dynamicColor = dynColorState,
        customSeedColor = effectiveSeedColor,
    ) {
        ApiClient.baseUrl = settings.apiBaseUrl
        ApiClient.secret = settings.apiSecret

        if (!isConfigured) {
            Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
                InitialSetupPage(settings) {
                    isConfigured = true
                    ApiClient.baseUrl = settings.apiBaseUrl
                    ApiClient.secret = settings.apiSecret
                }
            }
        } else {
            NekoPanelMain(
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

@Composable
fun NekoPanelMain(
    settings: SettingsManager,
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCustomColorChange: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var trafficTab by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableLongStateOf(0L) }
    var currentMode by remember { mutableStateOf("rule") }
    var currentLogLevel by remember { mutableStateOf(settings.logLevel) }
    var blurStyle by remember { mutableIntStateOf(settings.topBarBlurStyle) }
    var enableBlur by remember { mutableStateOf(settings.enableBlur) }
    var transitionStyle by remember { mutableIntStateOf(settings.transitionStyle) }

    LaunchedEffect(Unit) {
        try { currentMode = ApiClient.getConfigs().optString("mode", "rule") } catch (_: Exception) {}
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (settings.backgroundWebSocket || settings.autoStartService) DataDaemonService.start(context)
    }

    val wsState = rememberWebSocketState(
        settings = settings,
        apiBaseUrl = ApiClient.baseUrl,
        apiSecret = ApiClient.secret,
        logLevel = currentLogLevel,
        selectedTab = selectedTab,
    )

    val memHistory = rememberChartHistory(wsState.globalInUse)
    val downHistory = rememberChartHistory(wsState.globalDown)

    val appState = remember(enableBlur, transitionStyle, selectedTab, trafficTab, currentMode, currentLogLevel, blurStyle) {
        AppState(
            enableBlur = enableBlur,
            transitionStyle = transitionStyle,
            selectedTab = selectedTab,
            trafficTab = trafficTab,
            currentMode = currentMode,
            currentLogLevel = currentLogLevel,
            blurStyle = blurStyle,
        )
    }
    val updateAppState: ((AppState) -> AppState) -> Unit = { /* state updated via individual vars */ }

    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember { Navigator(backStack) }

    val navCornerRadius = rememberNavSystemCornerRadius()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val effects = remember(navCornerRadius, surfaceColor) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = navCornerRadius,
            cornerClipMode = NavCornerClipMode.Leading,
            dimAmount = 0.5f,
            backdropColor = surfaceColor,
        )
    }
    val isCrossActivityStyle = transitionStyle == 1
    val navTransition = if (isCrossActivityStyle) CrossActivityTransition else NavTransitions.MiuixDefault
    val swipeBackDirection = if (isCrossActivityStyle) NavSwipeDirection.None else NavSwipeDirection.LeftToRight

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppState provides appState,
        LocalUpdateAppState provides updateAppState,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) navigator.pop() },
            transition = navTransition,
            effects = effects,
        ) {
            entry<Route.Main>(swipeDismiss = swipeBackDirection) {
                MainScreenContent(
                    settings = settings,
                    selectedTab = selectedTab,
                    trafficTab = trafficTab,
                    refreshTick = refreshTick,
                    currentMode = currentMode,
                    currentLogLevel = currentLogLevel,
                    wsState = wsState,
                    memHistory = memHistory,
                    downHistory = downHistory,
                    onTabSelected = { selectedTab = it },
                    onTrafficTabSelected = { trafficTab = it },
                    onModeChange = { currentMode = it },
                    onLevelChange = { currentLogLevel = it; settings.logLevel = it },
                    onRefresh = { refreshTick = System.currentTimeMillis() },
                    onNavi = { navigator.push(it) },
                )
            }
            entry<Route.UiSettings>(swipeDismiss = swipeBackDirection) {
                Surface(Modifier.fillMaxSize()) {
                    UiSettingsScreen(
                        settings,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onCustomColorChange = onCustomColorChange,
                        onBlurStyleChange = { blurStyle = it; settings.topBarBlurStyle = it },
                        onTransitionStyleChange = { transitionStyle = it; settings.transitionStyle = it },
                        onEnableBlurChange = { enableBlur = it; settings.enableBlur = it },
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.Backup>(swipeDismiss = swipeBackDirection) {
                Surface(Modifier.fillMaxSize()) {
                    BackupScreen(settings, onBack = { navigator.pop() })
                }
            }
        }
    }
}

@Composable
internal fun MainScreenContent(
    settings: SettingsManager,
    selectedTab: Int,
    trafficTab: Int,
    refreshTick: Long,
    currentMode: String,
    currentLogLevel: String,
    wsState: WebSocketState,
    memHistory: List<Long>,
    downHistory: List<Long>,
    onTabSelected: (Int) -> Unit,
    onTrafficTabSelected: (Int) -> Unit,
    onModeChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onNavi: (NavKey) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val isTrafficTab = selectedTab == 2
    val effectiveScrollBehavior = if (isTrafficTab) null else scrollBehavior
    val enableBlur = LocalAppState.current.enableBlur
    val blurStyle = LocalAppState.current.blurStyle
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val isProgressiveBlur = blurActive && blurStyle == 1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isTrafficTab) {
                Box(
                    modifier = Modifier
                        .then(
                            when {
                                isProgressiveBlur -> Modifier
                                blurActive -> Modifier.textureBlur(
                                    backdrop = backdrop,
                                    shape = RectangleShape,
                                    blurRadius = 25f,
                                    colors = BlurDefaults.blurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.2f)),
                                        ),
                                    ),
                                )
                                else -> Modifier
                            },
                        )
                        .background(barColor),
                ) {
                    if (isProgressiveBlur) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = effectiveScrollBehavior?.state
                                        ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                                        ?: 1f
                                }
                                .progressiveTextureBlur(
                                    backdrop = backdrop,
                                    shape = RectangleShape,
                                    gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                                    blurRadius = 10f,
                                    colors = BlurDefaults.blurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.15f)),
                                        ),
                                    ),
                                ),
                        )
                    }
                    TopAppBar(
                        title = when (selectedTab) { 0 -> "代理"; 1 -> "规则"; 3 -> "设置"; else -> "" },
                        scrollBehavior = effectiveScrollBehavior,
                        color = barColor,
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .then(
                        if (blurActive) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                blurRadius = 25f,
                                colors = BlurDefaults.blurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.2f)),
                                    ),
                                ),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .background(barColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                NavigationBar(color = barColor) {
                    listOf(
                        "代理" to Icons.AutoMirrored.Filled.List,
                        "规则" to Icons.Default.CheckCircle,
                        "监控" to Icons.Default.SwapCalls,
                        "设置" to Icons.Default.Settings,
                    ).forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            icon = icon,
                            label = label,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            Column(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                if (isTrafficTab) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        TabRowWithContour(
                            tabs = listOf("概览", "连接", "日志"),
                            selectedTabIndex = trafficTab,
                            onTabSelected = { onTrafficTabSelected(it) },
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ProxiesScreen(settings, refreshTick, currentMode, onRefresh = onRefresh, onModeChange = onModeChange)
                        1 -> RulesScreen(refreshTick, settings)
                        2 -> TrafficScreen(
                            trafficTab, wsState.logs, wsState.connections, settings, currentLogLevel,
                            wsState.globalInUse, wsState.globalDown, wsState.totalDown, wsState.totalUp,
                            memHistory, downHistory,
                            onLevelChange = onLevelChange,
                            onRemoveConnection = { wsState.removeConnection(it) },
                            onClearConnections = { wsState.clearConnections() },
                        )
                        3 -> FullSettingsScreen(
                            settings,
                            onNavigateToUiSettings = { onNavi(Route.UiSettings) },
                            onNavigateToBackup = { onNavi(Route.Backup) },
                        )
                    }
                }
            }
        }
    }
}
