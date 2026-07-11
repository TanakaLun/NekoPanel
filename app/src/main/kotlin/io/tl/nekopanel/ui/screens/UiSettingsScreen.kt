package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.theme.JapaneseThemeSchemes

@Composable
fun UiSettingsScreen(
    settings: SettingsManager, onPureBlackToggle: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit = {}, onDynamicColorChange: (Boolean) -> Unit = {}, onCustomColorChange: (String) -> Unit = {},
    onBackAnimChange: (String) -> Unit = {},
    onBack: () -> Unit
) {
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
                            SliderPreference("圆角弧度", radiusState, onValueChange = { radiusState = it; settings.badgeCornerRadius = it })
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
