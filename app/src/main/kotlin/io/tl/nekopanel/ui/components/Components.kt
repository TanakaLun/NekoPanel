package io.tl.nekopanel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.SettingsManager
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.*
import org.json.JSONObject

// ---------- 数据模型 ----------
data class ConnectionItem(
    val id: String,
    val host: String,
    val network: String,
    val proxy: String,
    val upload: Long,
    val download: Long,
    val rawJson: String
)

data class ConnectionDetail(
    val target: String,
    val process: String,
    val networkInfo: String,
    val routeNode: String,
    val rule: String,
    val startTimeMillis: Long
)

data class LogItem(
    val type: String,
    val payload: String,
    val time: Long = System.currentTimeMillis()
)

fun Long.formatSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)
    return String.format(
        java.util.Locale.getDefault(),
        "%.2f %s",
        this / 1024.0.pow(digitGroups),
        units[digitGroups]
    )
}

// ---------- 图表 ----------
@Composable
fun MiniLineChart(data: List<Long>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val maxVal = data.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val minVal = data.minOrNull() ?: 0L
        val range = (maxVal - minVal).coerceAtLeast(1L)
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)

        fun getX(index: Int) = index * spacing
        fun getY(value: Long) = height - ((value - minVal).toFloat() / range * height)

        val strokePath = Path().apply {
            moveTo(getX(0), getY(data[0]))
            for (i in 0 until data.size - 1) {
                val x1 = getX(i); val y1 = getY(data[i])
                val x2 = getX(i + 1); val y2 = getY(data[i + 1])
                cubicTo(x1 + (x2 - x1) / 2f, y1, x1 + (x2 - x1) / 2f, y2, x2, y2)
            }
        }
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(getX(data.size - 1), height)
            lineTo(getX(0), height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(strokePath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun rememberChartHistory(currentValue: Long): List<Long> {
    val history = remember { mutableStateListOf<Long>() }
    LaunchedEffect(currentValue) {
        history.add(currentValue)
        if (history.size > 50) history.removeAt(0)
    }
    return history
}

// ---------- 顶部导航 ----------
@Composable
fun CapsuleTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit, tabs: List<String>) {
    Surface(modifier = Modifier.wrapContentWidth().height(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val textScale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")
                    Box(
                        modifier = Modifier.height(32.dp).wrapContentWidth().clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTabSelected(index) }
                            .padding(horizontal = 16.dp), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            modifier = Modifier.graphicsLayer { scaleX = textScale; scaleY = textScale }
                        )
                    }
                }
            }
        }
    }
}

// ---------- 下拉选择器 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSpinner(currentMode: String, onModeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("rule" to "规则模式", "global" to "全局模式", "direct" to "直连模式")
    
    Box {
        FilterChip(
            selected = true, 
            onClick = { expanded = true },
            label = { Text(modes.find { it.first == currentMode }?.second ?: currentMode, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(150.dp).background(MaterialTheme.colorScheme.surface),
                offset = DpOffset(0.dp, 4.dp)
            ) {
                modes.forEach { (key, label) ->
                    val isSelected = currentMode == key
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        onClick = { 
                            onModeSelected(key)
                            expanded = false 
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        colors = MenuDefaults.itemColors(
                            textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            trailingIconColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSpinner(currentLevel: String, onLevelSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val levels = listOf("info", "warning", "error", "debug", "silent")
    
    Box {
        FilterChip(
            selected = true, 
            onClick = { expanded = true },
            label = { Text(currentLevel.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(140.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = currentLevel == level
                    DropdownMenuItem(
                        text = { Text(level.uppercase(), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { 
                            onLevelSelected(level)
                            expanded = false 
                        },
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                        colors = MenuDefaults.itemColors(
                            textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownMenuInline(label: String, currentValue: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        
        Box(modifier = Modifier.width(160.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentValue, 
                    onValueChange = {}, 
                    readOnly = true,
                    textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(), 
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(18.dp))) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        options.forEach { option ->
                            val isSelected = currentValue == option
                            DropdownMenuItem(
                                text = { Text(option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { 
                                    onSelected(option)
                                    expanded = false 
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent),
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Badge ----------
@Composable
fun TypeBadge(text: String, style: String, cornerRadius: Int, isFixedSize: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        color = if (style == "填充") color else Color.Transparent, shape = RoundedCornerShape(cornerRadius.dp),
        border = if (style == "描边") BorderStroke(1.dp, color) else null,
        modifier = if (isFixedSize) Modifier.width(60.dp).height(20.dp) else Modifier.wrapContentSize()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = if (!isFixedSize) Modifier.padding(horizontal = 6.dp, vertical = 2.dp) else Modifier.fillMaxSize()) {
            Text(text.uppercase(), style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, platformStyle = PlatformTextStyle(includeFontPadding = false)), color = if (style == "填充") Color.White else color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelayBadge(delay: Int, isTesting: Boolean, style: String, cornerRadius: Int, isFixedSize: Boolean, onClick: () -> Unit) {
    val color = when {
        isTesting -> MaterialTheme.colorScheme.primary
        delay <= 0 -> MaterialTheme.colorScheme.outline
        delay < 200 -> Color(0xFF388E3C)
        delay < 500 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }
    val display = if (isTesting) "......" else if (delay <= 0) "TEST" else "${delay}ms"
    Surface(
        modifier = (if (isFixedSize) Modifier.width(60.dp).height(20.dp) else Modifier.wrapContentSize())
            .clip(RoundedCornerShape(cornerRadius.dp))
            .clickable(enabled = !isTesting, onClick = onClick, indication = ripple(), interactionSource = remember { MutableInteractionSource() }),
        color = if (style == "填充") (if (isTesting) color.copy(0.6f) else color) else Color.Transparent,
        shape = RoundedCornerShape(cornerRadius.dp),
        border = if (style == "描边") BorderStroke(1.dp, color.copy(if (isTesting) 0.5f else 1f)) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = if (!isFixedSize) Modifier.padding(horizontal = 6.dp, vertical = 2.dp) else Modifier.fillMaxSize()) {
            Text(display, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, platformStyle = PlatformTextStyle(includeFontPadding = false)), color = if (style == "填充") Color.White else color)
        }
    }
}

// ---------- 代理图标 ----------
@Composable
fun ProxyIconContainer(url: String?, fallbackText: String) {
    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (url.isNullOrBlank()) MaterialTheme.colorScheme.primaryContainer else Color.Transparent), contentAlignment = Alignment.Center) {
        if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(24.dp))
        else Text(fallbackText.take(1).uppercase(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// ---------- 开关 ----------
@Composable
fun ConfigToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ---------- 节点卡片 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeCard(name: String, type: String, lastDelay: Int, isSelected: Boolean, isTesting: Boolean, settings: SettingsManager, onClick: () -> Unit, onRefreshDelay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (settings.cardFillStyle) MaterialTheme.colorScheme.surfaceVariant.copy(0.4f) else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))) {
        Box(Modifier.padding(10.dp).fillMaxWidth().height(54.dp)) {
            Text(name, Modifier.align(Alignment.TopStart).fillMaxWidth().basicMarquee(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Text(type, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                DelayBadge(lastDelay, isTesting, settings.delayBadgeStyle, settings.badgeCornerRadius, false, onRefreshDelay)
            }
        }
    }
}

// ---------- 节点网格 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeGridSection(
    groupName: String,
    nodes: List<String>,
    currentNode: String,
    initialDelays: Map<String, Int> = emptyMap(),
    settings: SettingsManager,
    onDelayUpdate: (String, Int) -> Unit,
    onNodeSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.columnCount),
        modifier = Modifier.padding(top = 16.dp).heightIn(max = 450.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(nodes, key = { _, name -> "$groupName-$name" }) { _, nodeName ->
            var isNodeTesting by remember { mutableStateOf(false) }

            val displayDelay = initialDelays[nodeName] ?: 0

            NodeCard(
                name = nodeName,
                type = "Proxy",
                lastDelay = displayDelay,
                isSelected = nodeName == currentNode,
                isTesting = isNodeTesting,
                settings = settings,
                onClick = { onNodeSelected(nodeName) },
                onRefreshDelay = {
                    scope.launch {
                        isNodeTesting = true
                        try {
                            val result = ApiClient.getProxyDelay(
                                nodeName,
                                settings.testUrl,
                                settings.testTimeout
                            )
                            val delay = result.optInt("delay", 0)
                            onDelayUpdate(nodeName, delay)
                        } catch (_: Exception) {
                        } finally {
                            isNodeTesting = false
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyGroupCard(
    name: String,
    group: JSONObject,
    now: String,                                 // 当前选中的节点名
    delayCache: Map<String, Int>,                 // 全局延迟缓存
    settings: SettingsManager,
    onDelayUpdate: (String, Int) -> Unit,         // 延迟更新回调
    onNodeSelected: (String) -> Unit,             // 节点选中回调
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val type = group.optString("type", "Unknown")
    val icon = group.optString("icon", null)
    val allNodes = mutableListOf<String>()
    val allArray = group.optJSONArray("all")
    if (allArray != null) for (i in 0 until allArray.length()) allNodes.add(allArray.getString(i))

    // 当前选中节点的延迟
    val currentDelay = delayCache[now] ?: 0

    val usePopup = settings.groupColumnCount == 2 || settings.useSheetMode

    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        onClick = { if (usePopup) isExpanded = true else isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = if (settings.cardFillStyle) MaterialTheme.colorScheme.surfaceVariant.copy(0.3f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProxyIconContainer(url = icon, fallbackText = name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(now, modifier = Modifier.basicMarquee(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                }
                if (settings.groupColumnCount == 1) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TypeBadge(type, settings.groupBadgeStyle, settings.badgeCornerRadius, false)
                        DelayBadge(currentDelay, isTesting, settings.delayBadgeStyle, settings.badgeCornerRadius, false) {
                            scope.launch {
                                isTesting = true
                                try {
                                    val delays = ApiClient.getGroupDelay(name, settings.testUrl, settings.testTimeout)
                                    val keys = delays.keys()
                                    while (keys.hasNext()) {
                                        val node = keys.next()
                                        val d = delays.getInt(node)
                                        onDelayUpdate(node, d)
                                    }
                                } catch (_: Exception) {}
                                finally {
                                    isTesting = false
                                }
                            }
                        }
                    }
                }
            }
            if (settings.groupColumnCount == 2) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    TypeBadge(type, settings.groupBadgeStyle, settings.badgeCornerRadius, true)
                    DelayBadge(currentDelay, isTesting, settings.delayBadgeStyle, settings.badgeCornerRadius, true) {
                        scope.launch {
                            isTesting = true
                            try {
                                val delays = ApiClient.getGroupDelay(name, settings.testUrl, settings.testTimeout)
                                val keys = delays.keys()
                                while (keys.hasNext()) {
                                    val node = keys.next()
                                    val d = delays.getInt(node)
                                    onDelayUpdate(node, d)
                                }
                            } catch (_: Exception) {}
                            finally {
                                isTesting = false
                            }
                        }
                    }
                }
            }
            if (!usePopup) {
                AnimatedVisibility(visible = isExpanded) {
                    NodeGridSection(
                        groupName = name,
                        nodes = allNodes,
                        currentNode = now,
                        initialDelays = delayCache,
                        settings = settings,
                        onDelayUpdate = onDelayUpdate,
                        onNodeSelected = onNodeSelected,
                    )
                }
            }
        }
    }

    if (usePopup && isExpanded) {
        if (settings.useSheetMode) {
            ModalBottomSheet(onDismissRequest = { isExpanded = false }) {
                Box(Modifier.padding(16.dp).fillMaxHeight(0.7f)) {
                    NodeGridSection(
                        groupName = name,
                        nodes = allNodes,
                        currentNode = now,
                        initialDelays = delayCache,
                        settings = settings,
                        onDelayUpdate = onDelayUpdate,
                        onNodeSelected = onNodeSelected,
                    )
                }
            }
        } else {
            Dialog(onDismissRequest = { isExpanded = false }) {
                Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        NodeGridSection(
                            groupName = name,
                            nodes = allNodes,
                            currentNode = now,
                            initialDelays = delayCache,
                            settings = settings,
                            onDelayUpdate = onDelayUpdate,
                            onNodeSelected = onNodeSelected,
                        )
                    }
                }
            }
        }
    }
}

// ---------- 连接卡片 ----------
// @Composable
// fun ConnectionCard(conn: ConnectionItem, onClick: () -> Unit, onClose: () -> Unit) {
    // val cardShape = RoundedCornerShape(20.dp)
    // val startTimeMillis = remember(conn.rawJson) {
        // try {
            // val startStr = JSONObject(conn.rawJson).optString("start", "")
            // if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // java.time.Instant.parse(startStr).toEpochMilli()
            // } else {
                // java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(startStr)?.time ?: System.currentTimeMillis()
            // }
        // } catch (e: Exception) { System.currentTimeMillis() }
    // }

    // Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(cardShape).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
        // Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Column(modifier = Modifier.weight(1f)) {
                // Text(text = conn.host, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                // Spacer(Modifier.height(4.dp))
                // Text(text = "${conn.network.uppercase()} · ${conn.proxy}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            // }
            // Spacer(Modifier.width(12.dp))
            // Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(IntrinsicSize.Max)) {
                // Row(verticalAlignment = Alignment.CenterVertically) {
                    // DurationBadge(startTimeMillis)
                    // Spacer(Modifier.width(6.dp))
                    // IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "断开连接", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(16.dp)) }
                // }
                // Spacer(Modifier.height(6.dp))
                // Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon(Icons.Default.ArrowUpward, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                    // Text(text = conn.upload.formatSize(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
                    // Spacer(Modifier.width(8.dp))
                    // Icon(Icons.Default.ArrowDownward, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                    // Text(text = conn.download.formatSize(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
                // }
            // }
        // }
    // }
// }

@Composable
fun DurationBadge(startTimeMillis: Long) {
    val durationText by produceState(initialValue = "...", startTimeMillis) {
        while (true) {
            val diffSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
            value = when {
                diffSeconds < 60 -> "${diffSeconds}s"
                diffSeconds < 3600 -> "${diffSeconds / 60}m"
                else -> "${diffSeconds / 3600}h"
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = CircleShape) {
        Text(text = durationText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
fun ConnectionCard(conn: ConnectionItem, onClick: () -> Unit, onClose: () -> Unit) {
    // 显式指定 remember 的类型，解决类型推断 T 失败的问题
    val detail = remember(conn.rawJson) {
        try {
            val json = JSONObject(conn.rawJson)
            val metadata = json.optJSONObject("metadata") ?: JSONObject()
            
            // 目标地址优先级：host > sniffHost > destinationIP
            val host = metadata.optString("host", "")
            val sniff = metadata.optString("sniffHost", "")
            val ip = metadata.optString("destinationIP", "")
            val targetStr = host.ifEmpty { sniff.ifEmpty { ip } }
            
            // 进程处理
            val processName = metadata.optString("process", "Unknown")
                .substringAfterLast("/")
                .substringAfterLast("\\")
            
            // 网络信息
            val net = metadata.optString("network", "TCP").uppercase()
            val type = metadata.optString("type", "DIRECT").uppercase()
            
            // 路由与节点
            val chains = json.optJSONArray("chains")
            val node = if (chains != null && chains.length() > 0) chains.optString(0) else "DIRECT"
            
            // 匹配规则
            val ruleStr = json.optString("rulePayload", json.optString("rule", "Match"))

            // 时间解析 (兼容旧版 API)
            val startStr = json.optString("start", "")
            val time = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.Instant.parse(startStr).toEpochMilli()
                } else {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(startStr)?.time ?: System.currentTimeMillis()
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            ConnectionDetail(
                target = targetStr,
                process = processName,
                networkInfo = "$net · $type",
                routeNode = node,
                rule = ruleStr,
                startTimeMillis = time
            )
        } catch (e: Exception) {
            ConnectionDetail(conn.host, "Error", "TCP", "Unknown", "", System.currentTimeMillis())
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 第一行：标题与关闭按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = detail.target,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee()
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 第二行：协议、进程、路由节点
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 协议 Badge
                Text(
                    text = detail.networkInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                
                Spacer(Modifier.width(8.dp))
                
                // 进程图标与名称
                Text(
                    text = detail.process,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(0.7f)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )

                // 路由节点
                Text(
                    text = detail.routeNode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .widthIn(max = 100.dp)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(8.dp))

            // 第三行：上下行流量与时长统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrafficStatItem(
                        symbol = "↑", 
                        value = conn.upload.formatSize(), 
                        color = MaterialTheme.colorScheme.primary
                    )
                    TrafficStatItem(
                        symbol = "↓", 
                        value = conn.download.formatSize(), 
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                // 持续时间组件
                DurationBadge(detail.startTimeMillis)
            }
        }
    }
}

/**
 * 内部流量辅助组件
 */
@Composable
private fun TrafficStatItem(symbol: String, value: String, color: Color) {
    Text(
        text = "$symbol $value",
        style = MaterialTheme.typography.labelSmall,
        color = color, // 将颜色应用到整串文字，视觉对比更强
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(end = 12.dp)
    )
}


@Composable
fun highlightJson(jsonStr: String): AnnotatedString {
    val colorScheme = MaterialTheme.colorScheme
    
    return remember(jsonStr, colorScheme) {
        buildAnnotatedString {
            val keyColor = colorScheme.primary
            val stringColor = colorScheme.tertiary
            val numberColor = colorScheme.secondary
            val booleanColor = colorScheme.error
            val nullColor = colorScheme.error
            val punctuationColor = colorScheme.onSurfaceVariant

            var i = 0
            while (i < jsonStr.length) {
                when {
                    jsonStr[i] == '"' -> {
                        val start = i; i++
                        while (i < jsonStr.length) {
                            if (jsonStr[i] == '\\' && i + 1 < jsonStr.length) i += 2
                            else if (jsonStr[i] == '"') { i++; break }
                            else i++
                        }
                        val str = jsonStr.substring(start, i)
                        var lookAhead = i
                        while (lookAhead < jsonStr.length && jsonStr[lookAhead].isWhitespace()) lookAhead++
                        val isKey = lookAhead < jsonStr.length && jsonStr[lookAhead] == ':'
                        
                        withStyle(SpanStyle(color = if (isKey) keyColor else stringColor, fontWeight = if(isKey) FontWeight.Bold else FontWeight.Normal)) {
                            append(str)
                        }
                    }
                    jsonStr[i].isDigit() || jsonStr[i] == '-' -> {
                        val start = i
                        while (i < jsonStr.length && (jsonStr[i].isDigit() || jsonStr[i] in ".-eE")) i++
                        withStyle(SpanStyle(color = numberColor)) { append(jsonStr.substring(start, i)) }
                    }
                    jsonStr.startsWith("true", i) || jsonStr.startsWith("false", i) -> {
                        val bool = if (jsonStr.startsWith("true", i)) "true" else "false"
                        withStyle(SpanStyle(color = booleanColor, fontWeight = FontWeight.Medium)) { append(bool) }
                        i += bool.length
                    }
                    jsonStr.startsWith("null", i) -> {
                        withStyle(SpanStyle(color = nullColor)) { append("null") }; i += 4
                    }
                    else -> {
                        val isPunctuation = jsonStr[i] in "{}[],:"
                        withStyle(SpanStyle(color = if(isPunctuation) punctuationColor else Color.Unspecified)) {
                            append(jsonStr[i])
                        }
                        i++
                    }
                }
            }
        }
    }
}
