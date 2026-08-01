package io.tl.nekopanel.ui.screens

import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.tl.nekopanel.MainActivity
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.service.DataDaemonService
import io.tl.nekopanel.privileged.PrivilegedBackendType
import io.tl.nekopanel.privileged.PrivilegedTrafficManager
import io.tl.nekopanel.privileged.KeepAliveManager
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private enum class ChannelSwitchAction { ENABLE_PRIVILEGED, ENABLE_KEEPALIVE }

@Composable
fun FullSettingsScreen(
    settings: SettingsManager,
    contentPadding: PaddingValues = PaddingValues(),
    snackbarHostState: SnackbarHostState,
    onNavigateToUiSettings: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    var config by remember { mutableStateOf<JSONObject?>(null) }
    var backend by remember { mutableStateOf(ApiClient.backend) }
    var coreVersion by remember { mutableStateOf("正在获取...") }
    var connectFailed by remember { mutableStateOf(false) }
    var reconfigDialog by remember { mutableStateOf(false) }
    var showPrivilegeDialog by remember { mutableStateOf(false) }
    var bgWs by remember { mutableStateOf(settings.backgroundWebSocket) }
    var autoStart by remember { mutableStateOf(settings.autoStartService) }
    var notifPriority by remember { mutableStateOf(settings.notificationPriority) }
    var privilegedEnabled by remember { mutableStateOf(settings.privilegedServiceEnabled) }
    var privilegedType by remember { mutableStateOf(settings.privilegedServiceType) }
    var keepAliveEnabled by remember { mutableStateOf(settings.keepAliveEnabled) }
    var channelSwitchTo by remember { mutableStateOf<ChannelSwitchAction?>(null) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun startPrivileged(backend: PrivilegedBackendType, onFailure: () -> Unit = {}) {
        PrivilegedTrafficManager.start(
            context = context,
            backend = backend,
            baseUrl = settings.apiBaseUrl,
            secret = settings.apiSecret,
            notificationPriority = settings.notificationPriority,
        ) { result ->
            result.onSuccess { showMessage("特权流量服务已启动") }
                .onFailure {
                    showMessage(it.message ?: "特权服务启动失败")
                    onFailure()
                }
        }
    }

    fun requestEnablePrivileged() {
        PrivilegedTrafficManager.probeCapabilities { capabilities ->
            val preferred = PrivilegedBackendType.from(privilegedType)
            val available = when {
                capabilities.supports(preferred) -> preferred
                capabilities.shizuku -> PrivilegedBackendType.Shizuku
                capabilities.root -> PrivilegedBackendType.Root
                else -> null
            }
            if (available != null) {
                if (bgWs) DataDaemonService.stop(context)
                privilegedType = available.value
                settings.privilegedServiceType = available.value
                privilegedEnabled = true
                settings.privilegedServiceEnabled = true
                if (bgWs) startPrivileged(available) {
                    privilegedEnabled = false
                    settings.privilegedServiceEnabled = false
                    DataDaemonService.start(context)
                }
            } else {
                PrivilegedTrafficManager.requestShizukuPermission { granted ->
                    if (granted) {
                        if (bgWs) DataDaemonService.stop(context)
                        privilegedType = PrivilegedBackendType.Shizuku.value
                        settings.privilegedServiceType = privilegedType
                        privilegedEnabled = true
                        settings.privilegedServiceEnabled = true
                        if (bgWs) startPrivileged(PrivilegedBackendType.Shizuku) {
                            privilegedEnabled = false
                            settings.privilegedServiceEnabled = false
                            DataDaemonService.start(context)
                        }
                    } else {
                        showPrivilegeDialog = true
                    }
                }
            }
        }
    }

    fun startKeepAlive() {
        if (bgWs) DataDaemonService.stop(context)
        keepAliveEnabled = true
        settings.keepAliveEnabled = true
        if (bgWs) {
            KeepAliveManager.start(
                context = context,
                baseUrl = settings.apiBaseUrl,
                secret = settings.apiSecret,
                notificationPriority = notifPriority,
            ) { result ->
                result.onSuccess { showMessage("保活通知已启动") }
                    .onFailure {
                        showMessage(it.message ?: "保活通知启动失败")
                        keepAliveEnabled = false
                        settings.keepAliveEnabled = false
                        DataDaemonService.start(context)
                    }
            }
        }
    }

    fun requestKeepAlive() {
        if (KeepAliveManager.isAvailable()) {
            startKeepAlive()
        } else {
            PrivilegedTrafficManager.requestShizukuPermission { granted ->
                if (granted) startKeepAlive() else showPrivilegeDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            backend = ApiClient.detectBackend()
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
                config = JSONObject(config.toString()).also { it.put(key, value) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showMessage("更新失败") }
            }
        }
    }

    if (connectFailed && !reconfigDialog) reconfigDialog = true

    if (reconfigDialog) {
        var tmpUrl by remember { mutableStateOf(settings.apiBaseUrl) }
        var tmpSecret by remember { mutableStateOf(settings.apiSecret) }
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("连接失败", fontWeight = FontWeight.Black, style = MiuixTheme.textStyles.title3)
                    Text("无法连接到核心，请检查地址和密钥是否正确", style = MiuixTheme.textStyles.body2)
                    TextField(value = tmpUrl, onValueChange = { tmpUrl = it }, label = "API 地址", singleLine = true)
                    TextField(value = tmpSecret, onValueChange = { tmpSecret = it }, label = "密钥 (可选)", singleLine = true)
Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        Button(onClick = {
                            settings.apiBaseUrl = tmpUrl.trimEnd('/')
                            settings.apiSecret = tmpSecret
                            ApiClient.baseUrl = settings.apiBaseUrl
                            ApiClient.secret = settings.apiSecret
                            ApiClient.resetConnection()
                            reconfigDialog = false
                            connectFailed = false
                            config = null; coreVersion = "正在获取..."
                            scope.launch {
                                try {
                                    backend = ApiClient.detectBackend()
                                    coreVersion = ApiClient.getVersion().optString("version", "Unknown")
                                    config = ApiClient.getConfigs()
                                } catch (_: Exception) { coreVersion = "获取失败"; connectFailed = true }
                            }
                        }, colors = ButtonDefaults.buttonColorsPrimary()) { Text("重新连接") }
                    }
                }
            }
        }
    }

    OverlayDialog(
        show = showPrivilegeDialog,
        title = "需要特权授权",
        summary = "未检测到可用的 Shizuku 或 Root 权限。请启动 Shizuku 并授予 NekoPanel 权限，或在 Root 管理器中允许 Root 访问。",
        onDismissRequest = { showPrivilegeDialog = false },
    ) {
        TextButton(
            text = "知道了",
            onClick = { showPrivilegeDialog = false },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }

    OverlayDialog(
        show = channelSwitchTo != null,
        title = "切换通知渠道",
        summary = when (channelSwitchTo) {
            ChannelSwitchAction.ENABLE_PRIVILEGED -> "开启「启用特权服务」将关闭「Shizuku 保活通知」通知渠道，是否继续？"
            ChannelSwitchAction.ENABLE_KEEPALIVE -> "开启「Shizuku 保活通知」将关闭「启用特权服务」通知渠道，是否继续？"
            null -> ""
        },
        onDismissRequest = { channelSwitchTo = null },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = "取消",
                modifier = Modifier.weight(1f),
                onClick = { channelSwitchTo = null },
            )
            TextButton(
                text = "确定",
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val action = channelSwitchTo
                    channelSwitchTo = null
                    when (action) {
                        ChannelSwitchAction.ENABLE_PRIVILEGED -> {
                            KeepAliveManager.stop(context)
                            keepAliveEnabled = false
                            settings.keepAliveEnabled = false
                            requestEnablePrivileged()
                        }
                        ChannelSwitchAction.ENABLE_KEEPALIVE -> {
                            PrivilegedTrafficManager.stop(context, PrivilegedBackendType.from(privilegedType))
                            privilegedEnabled = false
                            settings.privilegedServiceEnabled = false
                            requestKeepAlive()
                        }
                        null -> {}
                    }
                },
            )
        }
    }

    if (config == null) {
        Box(Modifier.fillMaxSize().padding(contentPadding), Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val cfg = config!!
    val isMihomo = backend.isMihomo

    LazyColumn(
        modifier = Modifier.fillMaxSize().scrollEndHaptic(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("内核版本", fontWeight = FontWeight.Black, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Text(coreVersion, fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.title4)
                    }
                }
            }
        }

        item {
            SectionTitle("核心控制")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { scope.launch { ApiClient.reloadConfigs(); showMessage("配置已重载") } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重载配置")
                        }
                        Button(onClick = { scope.launch { ApiClient.restartCore(); showMessage("核心已重启") } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) {
                            Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重启核心")
                        }
                    }
                    Button(onClick = { scope.launch { ApiClient.flushDnsCache(); showMessage("DNS 缓存已清除") } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清除 DNS 缓存")
                    }
                    Button(onClick = { scope.launch { ApiClient.flushFakeipCache(); showMessage("FakeIP 池已清除") } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清除 FakeIP 池")
                    }
                }
            }
        }

        if (isMihomo) {
            item {
                SectionTitle("网络端口")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val ports = listOf("mixed-port" to "混合端口", "port" to "HTTP 端口", "socks-port" to "Socks 端口", "redir-port" to "Redir 端口", "tproxy-port" to "Tproxy 端口")
                        ports.forEach { (key, label) ->
                            var txt by remember(cfg) { mutableStateOf(cfg.optInt(key, 0).toString()) }
                            TextField(value = txt, onValueChange = { txt = it; it.toIntOrNull()?.let { v -> updateRemote(key, v) } }, label = label, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        var bindAddr by remember(cfg) { mutableStateOf(cfg.optString("bind-address", "*")) }
                        TextField(value = bindAddr, onValueChange = { bindAddr = it; updateRemote("bind-address", it) }, label = "绑定地址")
                    }
                }
            }
        }

        if (isMihomo) {
            item {
                val tun = cfg.optJSONObject("tun") ?: JSONObject()
                var tunEnable by remember(cfg) { mutableStateOf(tun.optBoolean("enable", false)) }
                var tunStack by remember(cfg) { mutableStateOf(tun.optString("stack", "system").lowercase()) }
                SectionTitle("TUN 模式")
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        ConfigToggle("启用 TUN", checked = tunEnable) { enabled ->
                            tunEnable = enabled
                            val newTun = tun.also { it.put("enable", enabled); it.put("stack", tunStack) }
                            updateRemote("tun", newTun)
                        }
                        if (tunEnable) {
                            SettingsDropdownMenuInline("堆栈选择", tunStack, listOf("system", "gvisor", "mixed")) { selected ->
                                tunStack = selected
                                val newTun = tun.also { it.put("stack", selected) }
                                updateRemote("tun", newTun)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("内核设置")
            Card(Modifier.fillMaxWidth()) {
                if (isMihomo) {
                    Column {
                        if (cfg.has("allow-lan")) ConfigToggle("允许局域网", checked = cfg.optBoolean("allow-lan", false)) { updateRemote("allow-lan", it) }
                        if (cfg.has("ipv6")) ConfigToggle("IPv6 支持", checked = cfg.optBoolean("ipv6", false)) { updateRemote("ipv6", it) }
                        if (cfg.has("sniffing")) ConfigToggle("流量嗅探", checked = cfg.optBoolean("sniffing", false)) { updateRemote("sniffing", it) }
                        if (cfg.has("unified-delay")) ConfigToggle("统一延迟", checked = cfg.optBoolean("unified-delay", false)) { updateRemote("unified-delay", it) }
                        if (cfg.has("tcp-concurrent")) ConfigToggle("TCP 并发", checked = cfg.optBoolean("tcp-concurrent", false)) { updateRemote("tcp-concurrent", it) }
                        if (cfg.has("traffic-cumulative")) ConfigToggle("记录总流量", checked = cfg.optBoolean("traffic-cumulative", false)) { updateRemote("traffic-cumulative", it) }
                    }
                } else {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "当前后端为 ${backend.displayName},其 clash-api 仅支持切换 mode,端口、TUN、allow-lan 等配置项无法通过 API 修改。",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("界面设置")
            Card(Modifier.fillMaxWidth()) {
                Column {
                    BasePreference(
                        title = "自定义主题、布局与显示偏好",
                        onClick = onNavigateToUiSettings,
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = MiuixTheme.colorScheme.outline)
                        }
                    )
                }
            }
        }

        item {
            SectionTitle("数据备份")
            Card(Modifier.fillMaxWidth()) {
                Column {
                    BasePreference(
                        title = "WebDAV / GitHub 远程备份",
                        onClick = onNavigateToBackup,
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = MiuixTheme.colorScheme.outline)
                        }
                    )
                }
            }
        }

        item {
            SectionTitle("流量监控")
            Card(Modifier.fillMaxWidth()) {
                Column {
                    ConfigToggle("后台流量监控", checked = bgWs) { enabled ->
                        bgWs = enabled; settings.backgroundWebSocket = enabled
                        if (enabled) {
                            when {
                                keepAliveEnabled -> KeepAliveManager.start(
                                    context = context,
                                    baseUrl = settings.apiBaseUrl,
                                    secret = settings.apiSecret,
                                    notificationPriority = notifPriority,
                                ) { result ->
                                    result.exceptionOrNull()?.let {
                                        showMessage(it.message ?: "保活通知启动失败")
                                        bgWs = false
                                        settings.backgroundWebSocket = false
                                    }
                                }
                                privilegedEnabled -> PrivilegedTrafficManager.start(
                                    context = context,
                                    backend = PrivilegedBackendType.from(privilegedType),
                                    baseUrl = settings.apiBaseUrl,
                                    secret = settings.apiSecret,
                                    notificationPriority = notifPriority,
                                ) { result ->
                                    result.exceptionOrNull()?.let {
                                        showMessage(it.message ?: "特权服务启动失败")
                                        bgWs = false
                                        settings.backgroundWebSocket = false
                                    }
                                }
                                else -> DataDaemonService.start(context)
                            }
                            if (!privilegedEnabled && !keepAliveEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                    MainActivity.requestBatteryExemption(context as android.app.Activity)
                                }
                            }
                        } else {
                            when {
                                keepAliveEnabled -> KeepAliveManager.stop(context)
                                privilegedEnabled -> PrivilegedTrafficManager.stop(context, PrivilegedBackendType.from(privilegedType))
                                else -> DataDaemonService.stop(context)
                            }
                        }
                    }
                    ConfigToggle("启用特权服务", checked = privilegedEnabled) { enabled ->
                        if (!enabled) {
                            PrivilegedTrafficManager.stop(context, PrivilegedBackendType.from(privilegedType))
                            if (bgWs) DataDaemonService.start(context)
                            privilegedEnabled = false
                            settings.privilegedServiceEnabled = false
                            return@ConfigToggle
                        }
                        if (keepAliveEnabled) {
                            channelSwitchTo = ChannelSwitchAction.ENABLE_PRIVILEGED
                        } else {
                            requestEnablePrivileged()
                        }
                    }
                    if (privilegedEnabled) {
                        val typeOptions = listOf("Shizuku", "Root")
                        val currentType = if (privilegedType == PrivilegedBackendType.Root.value) typeOptions[1] else typeOptions[0]
                        SettingsDropdownMenuInline("特权类型", currentType, typeOptions) { selected ->
                            val oldType = PrivilegedBackendType.from(privilegedType)
                            val target = if (selected == typeOptions[1]) PrivilegedBackendType.Root else PrivilegedBackendType.Shizuku
                            if (target == oldType) return@SettingsDropdownMenuInline

                            fun switchBackend() {
                                if (bgWs) PrivilegedTrafficManager.stop(context, oldType)
                                privilegedType = target.value
                                settings.privilegedServiceType = target.value
                                if (bgWs) startPrivileged(target) {
                                    privilegedType = oldType.value
                                    settings.privilegedServiceType = oldType.value
                                    startPrivileged(oldType)
                                }
                            }

                            if (PrivilegedTrafficManager.capabilities().supports(target)) {
                                switchBackend()
                            } else if (target == PrivilegedBackendType.Shizuku) {
                                PrivilegedTrafficManager.requestShizukuPermission { granted ->
                                    if (granted) switchBackend() else showPrivilegeDialog = true
                                }
                            } else {
                                PrivilegedTrafficManager.requestRootAccess { granted ->
                                    if (granted) switchBackend() else showMessage("Root 权限不可用，已保留当前特权类型")
                                }
                            }
                        }
                    }
                    ConfigToggle(
                        label = "Shizuku 保活通知",
                        description = "以应用自身名义发布通知，应用进程被杀后通知仍持续存活",
                        checked = keepAliveEnabled,
                    ) { enabled ->
                        if (!enabled) {
                            KeepAliveManager.stop(context)
                            if (bgWs) DataDaemonService.start(context)
                            keepAliveEnabled = false
                            settings.keepAliveEnabled = false
                            return@ConfigToggle
                        }
                        if (privilegedEnabled) {
                            channelSwitchTo = ChannelSwitchAction.ENABLE_KEEPALIVE
                        } else {
                            requestKeepAlive()
                        }
                    }
                    ConfigToggle("自启动流量监控", checked = autoStart) { autoStart = it; settings.autoStartService = it }
                    val notifOpts = listOf("优先实时流量", "优先总流量")
                    val curNotif = if (notifPriority == "total") "优先总流量" else "优先实时流量"
                    SettingsDropdownMenuInline("通知显示内容", curNotif, notifOpts) { s ->
                        notifPriority = if (s == "优先总流量") "total" else "speed"
                        settings.notificationPriority = notifPriority
                        if (!bgWs) {
                            showMessage("通知显示偏好已保存")
                        } else if (keepAliveEnabled) {
                            KeepAliveManager.updateNotificationPriority(notifPriority)
                        } else if (privilegedEnabled) {
                            PrivilegedTrafficManager.updateNotificationPriority(notifPriority)
                        } else {
                            DataDaemonService.refreshNotification(context, notifPriority)
                        }
                    }
                }
            }
        }

        if (settings.backgroundWebSocket && !settings.privilegedServiceEnabled && !settings.keepAliveEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item {
                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                val isExempt = pm.isIgnoringBatteryOptimizations(context.packageName)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isExempt) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (isExempt) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if (isExempt) "已免除电池优化限制" else "未获取后台运行权限，点击申请", style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Bold, color = if (isExempt) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error, modifier = Modifier.clickable { if (!isExempt) MainActivity.requestBatteryExemption(context as android.app.Activity) })
                    }
                }
            }
        }

        item {
            SectionTitle("连接设置")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var url by remember { mutableStateOf(settings.apiBaseUrl) }
                    var secret by remember { mutableStateOf(settings.apiSecret) }
                    TextField(value = url, onValueChange = { url = it }, label = "API 地址", singleLine = true)
                    TextField(value = secret, onValueChange = { secret = it }, label = "密钥", singleLine = true)
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        Button(onClick = {
                            settings.apiBaseUrl = url.trimEnd('/')
                            settings.apiSecret = secret
                            ApiClient.baseUrl = settings.apiBaseUrl
                            ApiClient.secret = settings.apiSecret
                            ApiClient.resetConnection()
                            showMessage("连接设置已保存，重启应用后生效")
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("保存并应用") }
                    }
                }
            }
        }
    }
}
