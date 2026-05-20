package io.tl.nekopanel.ui.screens
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.MainActivity
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.service.DataDaemonService
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.theme.JapaneseThemeSchemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun FullSettingsScreen(settings: SettingsManager, onPureBlackToggle: (Boolean) -> Unit, onNavigateToUiSettings: () -> Unit = {}, onNavigateToBackup: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var config by remember { mutableStateOf<JSONObject?>(null) }
    var coreVersion by remember { mutableStateOf("正在获取...") }
    var connectFailed by remember { mutableStateOf(false) }
    var reconfigDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            coreVersion = ApiClient.getVersion().optString("version", "Unknown")
            config = ApiClient.getConfigs()
        } catch (_: Exception) {
            coreVersion = "获取失败"
            connectFailed = true
        }
    }

    fun updateRemote(key: String, value: Any) {
        scope.launch(Dispatchers.IO) {
            try {
                ApiClient.updateConfigs(mapOf(key to value))
                config = config?.apply { put(key, value) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    if (connectFailed && !reconfigDialog) reconfigDialog = true

    if (reconfigDialog) {
        var tmpUrl by remember { mutableStateOf(settings.apiBaseUrl) }
        var tmpSecret by remember { mutableStateOf(settings.apiSecret) }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("连接失败") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("无法连接到核心，请检查地址和密钥是否正确", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = tmpUrl, onValueChange = { tmpUrl = it }, label = { Text("API 地址") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = tmpSecret, onValueChange = { tmpSecret = it }, label = { Text("密钥 (可选)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.apiBaseUrl = tmpUrl.trimEnd('/')
                    settings.apiSecret = tmpSecret
                    ApiClient.baseUrl = settings.apiBaseUrl
                    ApiClient.secret = settings.apiSecret
                    reconfigDialog = false
                    connectFailed = false
                    config = null; coreVersion = "正在获取..."
                    scope.launch {
                        try {
                            coreVersion = ApiClient.getVersion().optString("version", "Unknown")
                            config = ApiClient.getConfigs()
                        } catch (_: Exception) { coreVersion = "获取失败"; connectFailed = true }
                    }
                }) { Text("重新连接") }
            }
        )
    }

    if (config == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val cfg = config!!

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        // --- 内核版本 ---
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.2f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("内核版本", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(coreVersion, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // --- 核心控制 ---
        item {
            SectionTitle("核心控制")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { scope.launch { ApiClient.reloadConfigs(); Toast.makeText(context, "配置已重载", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重载配置")
                        }
                        Button(onClick = { scope.launch { ApiClient.restartCore(); Toast.makeText(context, "核心已重启", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重启核心")
                        }
                    }
                    Button(onClick = { scope.launch { ApiClient.flushDnsCache(); Toast.makeText(context, "DNS 缓存已清除", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清除 DNS 缓存")
                    }
                    Button(onClick = { scope.launch { ApiClient.flushFakeipCache(); Toast.makeText(context, "FakeIP 池已清除", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清除 FakeIP 池")
                    }
                }
            }
        }

        // --- 网络端口 ---
        item {
            SectionTitle("网络端口")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val ports = listOf("mixed-port" to "混合端口", "port" to "HTTP 端口", "socks-port" to "Socks 端口", "redir-port" to "Redir 端口", "tproxy-port" to "Tproxy 端口")
                    ports.forEach { (key, label) ->
                        var txt by remember(cfg) { mutableStateOf(cfg.optInt(key, 0).toString()) }
                        OutlinedTextField(value = txt, onValueChange = { txt = it; it.toIntOrNull()?.let { v -> updateRemote(key, v) } }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                    }
                    var bindAddr by remember(cfg) { mutableStateOf(cfg.optString("bind-address", "*")) }
                    OutlinedTextField(value = bindAddr, onValueChange = { bindAddr = it; updateRemote("bind-address", it) }, label = { Text("绑定地址") }, shape = RoundedCornerShape(12.dp))
                }
            }
        }

        // --- TUN 模式 ---
        item {
            val tun = cfg.optJSONObject("tun") ?: JSONObject()
            var tunEnable by remember(cfg) { mutableStateOf(tun.optBoolean("enable", false)) }
            var tunStack by remember(cfg) { mutableStateOf(tun.optString("stack", "system").lowercase()) }
            SplicedColumnGroup(title = "TUN 模式") {
                item {
                    ConfigToggle("启用 TUN", checked = tunEnable) { enabled ->
                        tunEnable = enabled
                        val newTun = tun.also { it.put("enable", enabled); it.put("stack", tunStack) }
                        updateRemote("tun", newTun)
                    }
                }
                item(key = "tun_stack", visible = tunEnable) {
                    SettingsDropdownMenuInline("堆栈选择", tunStack, listOf("system", "gvisor", "mixed")) { selected ->
                        tunStack = selected
                        val newTun = tun.also { it.put("stack", selected) }
                        updateRemote("tun", newTun)
                    }
                }
            }
        }

        // --- 内核设置 ---
        item {
            SplicedColumnGroup(title = "内核设置") {
                item { ConfigToggle("允许局域网", checked = cfg.optBoolean("allow-lan", false)) { updateRemote("allow-lan", it) } }
                item { ConfigToggle("IPv6 支持", checked = cfg.optBoolean("ipv6", false)) { updateRemote("ipv6", it) } }
                item { ConfigToggle("流量嗅探", checked = cfg.optBoolean("sniffing", false)) { updateRemote("sniffing", it) } }
                item { ConfigToggle("统一延迟", checked = cfg.optBoolean("unified-delay", false)) { updateRemote("unified-delay", it) } }
                item { ConfigToggle("TCP 并发", checked = cfg.optBoolean("tcp-concurrent", false)) { updateRemote("tcp-concurrent", it) } }
            }
        }

        // --- 界面设置 (导航) ---
        item {
            SplicedColumnGroup(title = "界面设置") {
                item {
                    BasePreference(
                        title = "自定义主题、布局与显示偏好",
                        onClick = onNavigateToUiSettings,
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    )
                }
            }
        }
        
        // --- 数据备份 ---
        item {
            SplicedColumnGroup(title = "数据备份") {
                item {
                    BasePreference(
                        title = "WebDAV / GitHub 远程备份",
                        onClick = onNavigateToBackup,
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    )
                }
            }
        }
        
        // --- 流量监控 ---
        item {
            var bgWs by remember { mutableStateOf(settings.backgroundWebSocket) }
            var autoStart by remember { mutableStateOf(settings.autoStartService) }
            SplicedColumnGroup(title = "流量监控") {
                item {
                    ConfigToggle("后台流量监控", checked = bgWs) { enabled ->
                        bgWs = enabled; settings.backgroundWebSocket = enabled
                        if (enabled) {
                            DataDaemonService.start(context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                    MainActivity.requestBatteryExemption(context as android.app.Activity)
                                }
                            }
                        } else {
                            DataDaemonService.stop(context)
                        }
                    }
                }
                item {
                    ConfigToggle("自启动流量监控", checked = autoStart) { autoStart = it; settings.autoStartService = it }
                }
            }
        }
        if (settings.backgroundWebSocket && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item {
                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                val isExempt = pm.isIgnoringBatteryOptimizations(context.packageName)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isExempt) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (isExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if (isExempt) "已免除电池优化限制" else "未获取后台运行权限，点击申请", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.clickable { if (!isExempt) MainActivity.requestBatteryExemption(context as android.app.Activity) })
                    }
                }
            }
        }
        
        // --- API 连接设置 ---
        item {
            SectionTitle("连接设置")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var url by remember { mutableStateOf(settings.apiBaseUrl) }
                    var secret by remember { mutableStateOf(settings.apiSecret) }
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("API 地址") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("密钥") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        Button(onClick = {
                            settings.apiBaseUrl = url.trimEnd('/')
                            settings.apiSecret = secret
                            ApiClient.baseUrl = settings.apiBaseUrl
                            ApiClient.secret = settings.apiSecret
                            Toast.makeText(context, "已保存，重启应用后生效", Toast.LENGTH_SHORT).show()
                        }) { Text("保存并应用") }
                    }
                }
            }
        }
    }
}

@Composable
fun UiSettingsScreen(
    settings: SettingsManager, onPureBlackToggle: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit = {}, onDynamicColorChange: (Boolean) -> Unit = {}, onCustomColorChange: (String) -> Unit = {},
    onBackAnimChange: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var groupColBy by remember { mutableStateOf(if(settings.groupColumnCount == 1) "1 列" else "2 列") }
    var nodeColBy by remember { mutableStateOf(if(settings.columnCount == 1) "1 列" else "2 列") }
    var gBadgeStyle by remember { mutableStateOf(settings.groupBadgeStyle) }
    var dBadgeStyle by remember { mutableStateOf(settings.delayBadgeStyle) }
    var rBadgeStyle by remember { mutableStateOf(settings.ruleBadgeStyle) }
    var showGlobalBy by remember { mutableStateOf(settings.showGlobal) }
    var useSheetBy by remember { mutableStateOf(settings.useSheetMode) }
    var cardFillBy by remember { mutableStateOf(settings.cardFillStyle) }
    var radiusState by remember { mutableIntStateOf(settings.badgeCornerRadius) }
    var pureState by remember { mutableStateOf(settings.pureBlackMode) }
    var themeModeState by remember { mutableStateOf(settings.themeMode) }
    var dynColorState by remember { mutableStateOf(settings.dynamicColorEnabled) }
    var customColorState by remember { mutableStateOf(settings.customThemeColorKey) }
    var backAnimState by remember { mutableStateOf(settings.backAnimStyle) }



    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
                Text("界面设置", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            }

            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column {
                    SectionTitle("实时预览")
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("代理组", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        TypeBadge("URL-TEST", gBadgeStyle, radiusState, false)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("延迟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        DelayBadge(120, false, dBadgeStyle, radiusState, false) {}
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("规则", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        TypeBadge("FINAL", rBadgeStyle, radiusState, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    SectionTitle("徽章样式")
                    SplicedColumnGroup {
                        item {
                            SettingsDropdownMenuInline("代理类型风格", gBadgeStyle, listOf("填充", "描边")) { gBadgeStyle = it; settings.groupBadgeStyle = it }
                        }
                        item {
                            SettingsDropdownMenuInline("延迟类型风格", dBadgeStyle, listOf("填充", "描边")) { dBadgeStyle = it; settings.delayBadgeStyle = it }
                        }
                        item {
                            SettingsDropdownMenuInline("规则类型风格", rBadgeStyle, listOf("填充", "描边")) { rBadgeStyle = it; settings.ruleBadgeStyle = it }
                        }
                        item {
                            SliderPreference("圆角弧度", radiusState) { radiusState = it; settings.badgeCornerRadius = it }
                        }
                    }
                }
            }

            item {
                SplicedColumnGroup(title = "布局设置") {
                    item {
                        SettingsDropdownMenuInline("代理组布局", groupColBy, listOf("1 列", "2 列")) { groupColBy = it; settings.groupColumnCount = if(it == "1 列") 1 else 2 }
                    }
                    item {
                        SettingsDropdownMenuInline("节点网格列数", nodeColBy, listOf("1 列", "2 列")) { nodeColBy = it; settings.columnCount = if(it == "1 列") 1 else 2 }
                    }
                    item {
                        val animNames = listOf("滑动", "缩放", "无")
                        val curAnim = when (backAnimState) { "scale" -> "缩放"; "none" -> "无"; else -> "滑动" }
                        SettingsDropdownMenuInline("返回动画", curAnim, animNames) { s ->
                            backAnimState = when (s) { "缩放" -> "scale"; "无" -> "none"; else -> "slide" }
                            onBackAnimChange(backAnimState)
                        }
                    }
                }
            }

            item {
                SplicedColumnGroup(title = "主题与行为") {
                    item {
                        val modeNames = listOf("跟随系统", "浅色模式", "深色模式")
                        val currentModeName = when (themeModeState) { "light" -> "浅色模式"; "dark" -> "深色模式"; else -> "跟随系统" }
                        SettingsDropdownMenuInline("外观模式", currentModeName, modeNames) { s ->
                            val newMode = when (s) { "浅色模式" -> "light"; "深色模式" -> "dark"; else -> "follow_system" }
                            themeModeState = newMode; onThemeModeChange(newMode)
                        }
                    }
                    item {
                        ConfigToggle("AMOLED 纯黑模式 (深色)", checked = pureState) { pureState = it; settings.pureBlackMode = it; onPureBlackToggle(it) }
                    }
                    item {
                        ConfigToggle("动态取色", checked = dynColorState) {
                            dynColorState = it; onDynamicColorChange(it)
                            if (!it && customColorState.isBlank()) {
                                customColorState = "akabeni"; onCustomColorChange("akabeni")
                            }
                        }
                    }
                    if (!dynColorState) {
                        item {
                            DropDownList(
                                label = "自定义主题色",
                                currentValue = customColorState,
                                displayValue = JapaneseThemeSchemes.firstOrNull { it.key == customColorState }?.name ?: "未选择",
                                options = JapaneseThemeSchemes.map { it.key },
                                onSelected = { customColorState = it; onCustomColorChange(it) },
                                itemContent = { key, selected ->
                                    val tc = JapaneseThemeSchemes.firstOrNull { it.key == key }!!
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(if (isSystemInDarkTheme()) tc.darkPrimary else tc.lightPrimary))
                                        Spacer(Modifier.width(12.dp))
                                        Text(tc.name, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SplicedColumnGroup(title = "代理组显示") {
                    item {
                        ConfigToggle("显示 GLOBAL 代理组", checked = showGlobalBy) { showGlobalBy = it; settings.showGlobal = it }
                    }
                    item {
                        ConfigToggle("点击开启底部抽屉模式", checked = useSheetBy) { useSheetBy = it; settings.useSheetMode = it }
                    }
                    item {
                        ConfigToggle("代理卡片扁平填充风格", checked = cardFillBy) { cardFillBy = it; settings.cardFillStyle = it }
                    }
                }
            }

        }
    }
}
}
