package io.tl.nekopanel.ui.screens

import android.widget.Toast
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
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.SettingsManager
import io.tl.nekopanel.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun FullSettingsScreen(settings: SettingsManager, onPureBlackToggle: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var config by remember { mutableStateOf<JSONObject?>(null) }
    var coreVersion by remember { mutableStateOf("正在获取...") }
    val cumulative = remember { settings.getCumulativeTraffic() }

    LaunchedEffect(Unit) {
        try {
            coreVersion = ApiClient.getVersion().optString("version", "Unknown")
            config = ApiClient.getConfigs()
        } catch (_: Exception) { coreVersion = "获取失败" }
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

    if (config == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val cfg = config!!

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        // --- API 连接设置 ---
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("连接设置", fontWeight = FontWeight.Black)
                    }
                    var url by remember { mutableStateOf(settings.apiBaseUrl) }
                    var secret by remember { mutableStateOf(settings.apiSecret) }
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("API 地址 (http://IP:PORT)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("密钥 (可选)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
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

        // --- 核心信息与操作 ---
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.2f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Terminal, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("核心控制", fontWeight = FontWeight.Black) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { scope.launch { ApiClient.reloadConfigs(); Toast.makeText(context, "配置已重载", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("重载配置")
                        }
                        Button(onClick = { scope.launch { ApiClient.restartCore(); Toast.makeText(context, "核心已重启", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("重启核心")
                        }
                    }
                }
            }
        }

        // --- 端口配置 ---
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("网络端口", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
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

        // --- TUN 配置（增强） ---
        item {
            val tun = cfg.optJSONObject("tun") ?: JSONObject()
            var tunEnable by remember(cfg) { mutableStateOf(tun.optBoolean("enable", false)) }
            var tunStack by remember(cfg) { mutableStateOf(tun.optString("stack", "system")) }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TUN 模式", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    ConfigToggle("启用 TUN", tunEnable) { enabled ->
                        tunEnable = enabled
                        val newTun = tun.also { it.put("enable", enabled) }
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

        // --- 其他核心开关 ---
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("内核设置", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    ConfigToggle("允许局域网", cfg.optBoolean("allow-lan", false)) { updateRemote("allow-lan", it) }
                    ConfigToggle("IPv6 支持", cfg.optBoolean("ipv6", false)) { updateRemote("ipv6", it) }
                    ConfigToggle("流量嗅探", cfg.optBoolean("sniffing", false)) { updateRemote("sniffing", it) }
                    ConfigToggle("统一延迟", cfg.optBoolean("unified-delay", false)) { updateRemote("unified-delay", it) }
                    ConfigToggle("TCP 并发", cfg.optBoolean("tcp-concurrent", false)) { updateRemote("tcp-concurrent", it) }
                    // 可继续添加 log-level, find-process-mode 等
                }
            }
        }

        item {
            // 定义本地状态以驱动 UI 实时刷新
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
            var bgWs by remember { mutableStateOf(settings.backgroundWebSocket) }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("界面设置", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    
                    // --- 新增：实时预览区 ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(60.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Badge 圆角弧度: ${radiusState}dp", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = radiusState.toFloat(), 
                        onValueChange = { 
                            radiusState = it.toInt()
                            settings.badgeCornerRadius = it.toInt()
                        }, 
                        valueRange = 0f..12f, 
                        steps = 12
                    )
                    
                    SettingsDropdownMenuInline("代理类型风格", gBadgeStyle, listOf("填充", "描边")) { 
                        gBadgeStyle = it
                        settings.groupBadgeStyle = it
                    }
                    SettingsDropdownMenuInline("延迟类型风格", dBadgeStyle, listOf("填充", "描边")) { 
                        dBadgeStyle = it
                        settings.delayBadgeStyle = it
                    }
                    SettingsDropdownMenuInline("规则类型风格", rBadgeStyle, listOf("填充", "描边")) { 
                        rBadgeStyle = it
                        settings.ruleBadgeStyle = it
                    }
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    
                    SettingsDropdownMenuInline("代理组布局", groupColBy, listOf("1 列", "2 列")) { 
                        groupColBy = it
                        settings.groupColumnCount = if(it == "1 列") 1 else 2
                    }
                    SettingsDropdownMenuInline("节点网格列数", nodeColBy, listOf("1 列", "2 列")) { 
                        nodeColBy = it
                        settings.columnCount = if(it == "1 列") 1 else 2
                    }
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    ConfigToggle("AMOLED 纯黑模式", pureState) { 
                        pureState = it
                        settings.pureBlackMode = it
                        onPureBlackToggle(it) 
                    }
                    
                    ConfigToggle("后台持续获取数据", bgWs) { 
                        bgWs = it
                        settings.backgroundWebSocket = it 
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    
                    ConfigToggle("显示 GLOBAL 代理组", showGlobalBy) { 
                        showGlobalBy = it
                        settings.showGlobal = it
                    }
                    ConfigToggle("点击开启底部抽屉模式", useSheetBy) { 
                        useSheetBy = it
                        settings.useSheetMode = it
                    }
                    ConfigToggle("代理卡片扁平填充风格", cardFillBy) { 
                        cardFillBy = it
                        settings.cardFillStyle = it
                    }
                }
            }
        }
    }
}