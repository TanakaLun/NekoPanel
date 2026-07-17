package io.tl.nekopanel.ui.screens

import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.MainActivity
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.service.DataDaemonService
import io.tl.nekopanel.service.RootDaemonManager
import io.tl.nekopanel.service.Shell
import io.tl.nekopanel.ui.components.*
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
            var notifPriority by remember { mutableStateOf(settings.notificationPriority) }
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
                item {
                    val notifOpts = listOf("优先实时流量", "优先总流量")
                    val curNotif = if (notifPriority == "total") "优先总流量" else "优先实时流量"
                    SettingsDropdownMenuInline("通知显示内容", curNotif, notifOpts) { s ->
                        notifPriority = if (s == "优先总流量") "total" else "speed"
                        settings.notificationPriority = notifPriority
                        DataDaemonService.refreshNotification(context)
                    }
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
        
        // --- 服务 (Root) ---
        item {
            var rootEnabled by remember { mutableStateOf(settings.rootDaemonEnabled) }
            var rootInterval by remember { mutableStateOf(settings.rootDaemonInterval) }
            var showRootDialog by remember { mutableStateOf(false) }
            SplicedColumnGroup(title = "服务") {
                item {
                    ConfigToggle("Root 守护进程", checked = rootEnabled) { en ->
                        if (en && !Shell.checkRootAccess()) {
                            showRootDialog = true
                            return@ConfigToggle
                        }
                        rootEnabled = en; settings.rootDaemonEnabled = en
                        if (en) {
                            RootDaemonManager.start(context)
                            Toast.makeText(context, "守护进程已启动", Toast.LENGTH_SHORT).show()
                        } else {
                            RootDaemonManager.stop()
                            Toast.makeText(context, "守护进程已停止", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (rootEnabled) {
                    item {
                        SettingsDropdownMenuInline("保活间隔", "${rootInterval}秒",
                            listOf("30秒", "60秒", "120秒", "300秒")) { s ->
                            rootInterval = s.dropLast(1).toIntOrNull() ?: 60
                            settings.rootDaemonInterval = rootInterval
                            RootDaemonManager.stop()
                            RootDaemonManager.start(context)
                        }
                    }
                }
            }
            if (showRootDialog) {
                AlertDialog(
                    onDismissRequest = { showRootDialog = false },
                    title = { Text("无 Root 权限") },
                    text = { Text("未检测到 Root 权限（su），无法启用守护进程。请确认设备已 Root 并授权。") },
                    confirmButton = { TextButton(onClick = { showRootDialog = false }) { Text("确定") } }
                )
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
