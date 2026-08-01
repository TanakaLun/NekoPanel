package io.tl.nekopanel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.basicMarquee
import coil.compose.AsyncImage
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.animation.AnimatedVisibility
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProxyIconContainer(url: String?, fallbackText: String) {
    Box(
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
            .background(if (url.isNullOrBlank()) MiuixTheme.colorScheme.primaryContainer else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(24.dp))
        } else {
            Text(
                fallbackText.take(1).uppercase(),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.ExtraBold,
                color = MiuixTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun NodeCard(
    name: String, type: String, lastDelay: Int, isSelected: Boolean, isTesting: Boolean,
    settings: SettingsManager, onClick: () -> Unit, onRefreshDelay: () -> Unit
) {
    val containerColor = when {
        isSelected -> MiuixTheme.colorScheme.primary
        settings.cardFillStyle -> MiuixTheme.colorScheme.surfaceVariant.copy(0.4f)
        else -> MiuixTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    val cardBorder = if (!isSelected && !settings.cardFillStyle) BorderStroke(0.5.dp, MiuixTheme.colorScheme.dividerLine.copy(0.5f)) else null
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).then(
            if (cardBorder != null) Modifier.border(cardBorder, RoundedCornerShape(14.dp)) else Modifier
        ),
        colors = CardDefaults.defaultColors(color = containerColor, contentColor = contentColor),
    ) {
        Box(Modifier.padding(10.dp).fillMaxWidth().height(54.dp)) {
            Text(
                name, Modifier.align(Alignment.TopStart).fillMaxWidth().basicMarquee(),
                style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.Bold, maxLines = 1,
                color = contentColor
            )
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Text(type, fontSize = 9.sp, color = contentColor.copy(alpha = 0.7f))
                DelayBadge(lastDelay, isTesting, settings.delayBadgeStyle, settings.badgeCornerRadius, false, onRefreshDelay)
            }
        }
    }
}

@Composable
fun NodeGridSection(
    groupName: String, nodes: List<String>, currentNode: String,
    initialDelays: Map<String, Int> = emptyMap(),
    settings: SettingsManager,
    onDelayUpdate: (String, Int) -> Unit, onNodeSelected: (String) -> Unit
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
                name = nodeName, type = "Proxy", lastDelay = displayDelay,
                isSelected = nodeName == currentNode, isTesting = isNodeTesting,
                settings = settings,
                onClick = { onNodeSelected(nodeName) },
                onRefreshDelay = {
                    scope.launch {
                        isNodeTesting = true
                        try {
                            val result = ApiClient.getProxyDelay(nodeName, settings.testUrl, settings.testTimeout)
                            val delay = result.optInt("delay", 0)
                            onDelayUpdate(nodeName, delay)
                        } catch (_: Exception) {} finally { isNodeTesting = false }
                    }
                }
            )
        }
    }
}

@Composable
fun ProxyGroupCard(
    name: String, group: JSONObject, now: String,
    delayCache: Map<String, Int>, settings: SettingsManager,
    onDelayUpdate: (String, Int) -> Unit, onNodeSelected: (String) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val type = group.optString("type", "Unknown")
    val icon: String? = group.optString("icon", null)
    val allNodes = mutableListOf<String>()
    val allArray = group.optJSONArray("all")
    if (allArray != null) for (i in 0 until allArray.length()) allNodes.add(allArray.getString(i))

    val currentDelay = delayCache[now] ?: 0
    val usePopup = settings.groupColumnCount == 2 || settings.useSheetMode

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        onClick = { if (usePopup) isExpanded = true else isExpanded = !isExpanded },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProxyIconContainer(url = icon, fallbackText = name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MiuixTheme.textStyles.subtitle, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(now, modifier = Modifier.basicMarquee(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline, maxLines = 1)
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
                                    while (keys.hasNext()) { val node = keys.next(); onDelayUpdate(node, delays.getInt(node)) }
                                } catch (_: Exception) {} finally { isTesting = false }
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
                                while (keys.hasNext()) { val node = keys.next(); onDelayUpdate(node, delays.getInt(node)) }
                            } catch (_: Exception) {} finally { isTesting = false }
                        }
                    }
                }
            }
            if (!usePopup) {
                AnimatedVisibility(visible = isExpanded) {
                    NodeGridSection(
                        groupName = name, nodes = allNodes, currentNode = now,
                        initialDelays = delayCache, settings = settings,
                        onDelayUpdate = onDelayUpdate, onNodeSelected = onNodeSelected,
                    )
                }
            }
        }
    }

    if (usePopup && isExpanded) {
        if (settings.useSheetMode) {
            top.yukonga.miuix.kmp.overlay.OverlayBottomSheet(show = isExpanded, onDismissRequest = { isExpanded = false }) {
                Box(Modifier.padding(16.dp).fillMaxHeight(0.7f)) {
                    NodeGridSection(
                        groupName = name, nodes = allNodes, currentNode = now,
                        initialDelays = delayCache, settings = settings,
                        onDelayUpdate = onDelayUpdate, onNodeSelected = onNodeSelected,
                    )
                }
            }
        } else {
            Dialog(onDismissRequest = { isExpanded = false }) {
                Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(name, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        NodeGridSection(
                            groupName = name, nodes = allNodes, currentNode = now,
                            initialDelays = delayCache, settings = settings,
                            onDelayUpdate = onDelayUpdate, onNodeSelected = onNodeSelected,
                        )
                    }
                }
            }
        }
    }
}