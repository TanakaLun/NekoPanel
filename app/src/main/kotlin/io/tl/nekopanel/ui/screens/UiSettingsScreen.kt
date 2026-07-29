package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.ui.components.*
import io.tl.nekopanel.ui.theme.AllThemeSchemes
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun UiSettingsScreen(
    settings: SettingsManager,
    onThemeModeChange: (String) -> Unit = {}, onDynamicColorChange: (Boolean) -> Unit = {}, onCustomColorChange: (String) -> Unit = {},
    onTransitionStyleChange: (Int) -> Unit = {},
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
    var themeModeState by remember { mutableStateOf(settings.themeMode) }
    var dynColorState by remember { mutableStateOf(settings.dynamicColorEnabled) }
    var customColorKey by remember { mutableStateOf(settings.customThemeColorKey) }
    var transitionStyleState by remember { mutableStateOf(settings.backAnimStyle.let { if (it == "scale" || it == "aosp") 1 else 0 }) }

    val schemeItems = remember {
        AllThemeSchemes.map { tc ->
            DropdownItem(
                text = tc.name,
                icon = { modifier ->
                    Box(modifier.clip(CircleShape).size(20.dp).background(tc.seedColor))
                },
            )
        }
    }
    val selectedSchemeIndex = remember(customColorKey) {
        AllThemeSchemes.indexOfFirst { it.key == customColorKey }.coerceAtLeast(0)
    }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MiuixTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
                Text("界面设置", fontWeight = FontWeight.Black, style = MiuixTheme.textStyles.title3)
            }

            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                                color = MiuixTheme.colorScheme.surfaceVariant.copy(0.3f),
                            ) {
                                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("代理组", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        TypeBadge("URL-TEST", gBadgeStyle, radiusState, false)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("延迟", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        DelayBadge(120, false, dBadgeStyle, radiusState, false) {}
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("规则", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        TypeBadge("FINAL", rBadgeStyle, radiusState, false)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("徽章样式")
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            SettingsDropdownMenuInline("代理类型风格", gBadgeStyle, listOf("填充", "描边")) { gBadgeStyle = it; settings.groupBadgeStyle = it }
                            SettingsDropdownMenuInline("延迟类型风格", dBadgeStyle, listOf("填充", "描边")) { dBadgeStyle = it; settings.delayBadgeStyle = it }
                            SettingsDropdownMenuInline("规则类型风格", rBadgeStyle, listOf("填充", "描边")) { rBadgeStyle = it; settings.ruleBadgeStyle = it }
                            SliderPreference("圆角弧度", radiusState, onValueChange = { radiusState = it; settings.badgeCornerRadius = it })
                        }
                    }
                }

                item {
                    SectionTitle("代理组显示")
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            SettingsDropdownMenuInline("代理组布局", groupColBy, listOf("1 列", "2 列")) { groupColBy = it; settings.groupColumnCount = if(it == "1 列") 1 else 2 }
                            SettingsDropdownMenuInline("节点网格列数", nodeColBy, listOf("1 列", "2 列")) { nodeColBy = it; settings.columnCount = if(it == "1 列") 1 else 2 }
                        }
                    }
                }

                item {
                    SectionTitle("主题与行为")
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            val modeNames = listOf("跟随系统", "浅色模式", "深色模式")
                            val currentModeName = when (themeModeState) { "light" -> "浅色模式"; "dark" -> "深色模式"; else -> "跟随系统" }
                            SettingsDropdownMenuInline("外观模式", currentModeName, modeNames) { s ->
                                val newMode = when (s) { "浅色模式" -> "light"; "深色模式" -> "dark"; else -> "follow_system" }
                                themeModeState = newMode; onThemeModeChange(newMode)
                            }
                            ConfigToggle("动态取色", checked = dynColorState) {
                                dynColorState = it; onDynamicColorChange(it)
                            }
                            if (!dynColorState) {
                                WindowSpinnerPreference(
                                    items = schemeItems,
                                    selectedIndex = selectedSchemeIndex,
                                    title = "主题色系",
                                    onSelectedIndexChange = { idx ->
                                        val key = AllThemeSchemes[idx].key
                                        customColorKey = key; onCustomColorChange(key)
                                    },
                                )
                            }
                        }
                    }
                }

                item {
                    SectionTitle("导航")
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            val transitionNames = listOf("Miuix", "AOSP")
                            val curTransition = if (transitionStyleState == 1) "AOSP" else "Miuix"
                            SettingsDropdownMenuInline("过渡动画风格", curTransition, transitionNames) { s ->
                                val newVal = if (s == "AOSP") 1 else 0
                                transitionStyleState = newVal; onTransitionStyleChange(newVal)
                            }
                        }
                    }
                }

                item {
                    SectionTitle("代理组显示")
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            ConfigToggle("显示 GLOBAL 代理组", checked = showGlobalBy) { showGlobalBy = it; settings.showGlobal = it }
                            ConfigToggle("点击开启底部抽屉模式", checked = useSheetBy) { useSheetBy = it; settings.useSheetMode = it }
                            ConfigToggle("代理卡片扁平填充风格", checked = cardFillBy) { cardFillBy = it; settings.cardFillStyle = it }
                        }
                    }
                }
            }
        }
    }
}