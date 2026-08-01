package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.model.RuleInfo
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.ChartWindow
import io.tl.nekopanel.ui.components.SectionTitle
import io.tl.nekopanel.ui.components.TrafficChart
import io.tl.nekopanel.ui.components.TypeBadge
import io.tl.nekopanel.util.formatSize
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun OverviewView(
    connections: List<ConnectionItem>,
    memoryInUse: Long,
    trafficUp: Long,
    trafficDown: Long,
    totalDown: Long,
    totalUp: Long,
    settings: SettingsManager,
    scaffoldPadding: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    val cumulative = remember { settings.getCumulativeTraffic() }
    val topRules = remember { mutableStateListOf<RuleInfo>() }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val rules = ApiClient.getRules()
                val filtered = rules.filter { it.hasHitStats && it.hitCount > 0 }
                topRules.clear()
                topRules.addAll(filtered.sortedByDescending { it.hitCount }.take(5))
            } catch (_: Exception) {}
            delay(5000)
        }
    }

    LazyColumn(
        modifier = Modifier.scrollEndHaptic(),
        contentPadding = PaddingValues(
            start = scaffoldPadding.calculateStartPadding(layoutDirection) + 16.dp,
            top = 16.dp,
            end = scaffoldPadding.calculateEndPadding(layoutDirection) + 16.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("系统概览")
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "内存占用 : ${memoryInUse.formatSize()}",
                        fontWeight = FontWeight.Bold,
                        style = MiuixTheme.textStyles.title4,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("上传速度", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Text("${trafficUp.formatSize()}/s", fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.title4)
                            ChartWindow(Modifier.fillMaxWidth().height(44.dp).padding(top = 4.dp)) {
                                TrafficChart(trafficUp, MiuixTheme.colorScheme.primary, Modifier.fillMaxSize())
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("下载速度", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Text("${trafficDown.formatSize()}/s", fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.title4)
                            ChartWindow(Modifier.fillMaxWidth().height(44.dp).padding(top = 4.dp)) {
                                TrafficChart(trafficDown, MiuixTheme.colorScheme.primary, Modifier.fillMaxSize())
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text("总下载", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                            Text(totalDown.formatSize(), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("总上传", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                            Text(totalUp.formatSize(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("累计流量", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("↓ ${cumulative.first.formatSize()}", fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                        Text("↑ ${cumulative.second.formatSize()}", fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                listOf("活跃连接" to "${connections.size}", "规则命中" to "${topRules.sumOf { it.hitCount }}").forEach { (label, value) ->
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                            Text(value, fontWeight = FontWeight.Black, style = MiuixTheme.textStyles.title4)
                        }
                    }
                }
            }
        }
        if (topRules.isNotEmpty()) {
            item { Text("高频规则命中", fontWeight = FontWeight.Black, style = MiuixTheme.textStyles.subtitle, modifier = Modifier.padding(top = 8.dp)) }
            items(topRules) { rule ->
                val type = rule.type
                val payload = rule.payload
                val proxy = rule.proxy
                val hitCount = rule.hitCount
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypeBadge(type, settings.ruleBadgeStyle, settings.badgeCornerRadius, false)
                                Spacer(Modifier.width(8.dp))
                                Text(payload, fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body2, lineHeight = 18.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("➔ $proxy", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                        }
                        Surface(color = MiuixTheme.colorScheme.primaryContainer, shape = CircleShape) {
                            Text("$hitCount", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.Black, color = MiuixTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
