package io.tl.nekopanel.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SplicedGroupScope.() -> Unit,
) {
    val scope = SplicedGroupScope().apply(content)
    val allItems = scope.items
    if (allItems.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
        }

        Column(verticalArrangement = Arrangement.Top) {
            val firstVisibleIndex = allItems.indexOfFirst { it.visible }
            val lastVisibleIndex = allItems.indexOfLast { it.visible }
            val sharedStiffness = Spring.StiffnessMediumLow

            allItems.forEachIndexed { index, itemData ->
                key(itemData.key) {
                    val zIndex = if (itemData.visible) 0f else 1f

                    AnimatedVisibility(
                        visible = itemData.visible,
                        modifier = Modifier.zIndex(zIndex),
                        enter = expandVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            expandFrom = Alignment.Top,
                        ) + fadeIn(
                            animationSpec = spring(stiffness = sharedStiffness),
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            shrinkTowards = Alignment.Top,
                        ) + fadeOut(
                            animationSpec = spring(stiffness = sharedStiffness),
                        ),
                    ) {
                        val isFirstVisible = index <= firstVisibleIndex && itemData.visible
                        val isLastVisible = index >= lastVisibleIndex && itemData.visible
                        var isPressed by remember { mutableStateOf(false) }

                        val targetTopRadius = if (isPressed) 16.dp else if (isFirstVisible) 16.dp else 2.dp
                        val targetBottomRadius = if (isPressed) 16.dp else if (isLastVisible) 16.dp else 2.dp

                        val isAtLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                        val currentTopRadius = if (isAtLeastTiramisu) {
                            animateDpAsState(
                                targetValue = targetTopRadius,
                                animationSpec = spring(stiffness = sharedStiffness),
                                label = "TopRadius",
                            ).value
                        } else targetTopRadius

                        val currentBottomRadius = if (isAtLeastTiramisu) {
                            animateDpAsState(
                                targetValue = targetBottomRadius,
                                animationSpec = spring(stiffness = sharedStiffness),
                                label = "BottomRadius",
                            ).value
                        } else targetBottomRadius

                        val shape = RoundedCornerShape(
                            topStart = currentTopRadius,
                            topEnd = currentTopRadius,
                            bottomStart = currentBottomRadius,
                            bottomEnd = currentBottomRadius,
                        )

                        val targetTopPadding = if (isFirstVisible) 0.dp else 2.dp
                        val currentTopPadding = if (isAtLeastTiramisu) {
                            animateDpAsState(
                                targetValue = targetTopPadding,
                                animationSpec = spring(stiffness = sharedStiffness),
                                label = "TopGap",
                            ).value
                        } else targetTopPadding

                        Column(
                            modifier = Modifier
                                .padding(top = currentTopPadding)
                                .pointerInput(itemData.key) {
                                    while (true) {
                                        awaitPointerEventScope {
                                            var event = awaitPointerEvent(PointerEventPass.Main)
                                            val firstChange = event.changes.firstOrNull() ?: return@awaitPointerEventScope
                                            if (!firstChange.pressed) return@awaitPointerEventScope
                                            val longPressMs = 400L
                                            val longPressActivated = withTimeoutOrNull(longPressMs) {
                                                while (true) {
                                                    val ev = awaitPointerEvent(PointerEventPass.Main)
                                                    val c = ev.changes.firstOrNull() ?: break
                                                    if (!c.pressed) return@withTimeoutOrNull false
                                                }
                                                false
                                            }
                                            if (longPressActivated == null) {
                                                isPressed = true
                                                while (true) {
                                                    event = awaitPointerEvent(PointerEventPass.Main)
                                                    val c = event.changes.firstOrNull() ?: break
                                                    isPressed = c.pressed
                                                    if (!c.pressed) break
                                                }
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    this.shape = shape
                                    this.clip = true
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            itemData.content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BasePreference(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    trailing: @Composable BoxScope.() -> Unit = {},
    onTapPosition: ((Float) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f

    val baseModifier = modifier
        .fillMaxWidth()
        .clickable(
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = if (onTapPosition != null) {
            baseModifier.pointerInput(onTapPosition) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.firstOrNull()?.let {
                            onTapPosition(it.position.x)
                        }
                    }
                }
            }
        } else baseModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Box(Modifier.alpha(alpha)) {
            trailing()
        }
    }
}

@Composable
fun ConfigToggle(
    label: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    BasePreference(
        title = label,
        description = description,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { onCheckedChange(it) },
            )
        }
    )
}

@Composable
fun SettingsDropdownMenuInline(
    label: String,
    currentValue: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var tapOffsetX by remember { mutableFloatStateOf(0f) }
    var parentWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { parentWidth = it.size.width }
    ) {
        BasePreference(
            title = label,
            modifier = modifier,
            onClick = { expanded = true },
            onTapPosition = { tapOffsetX = it },
            trailing = {
                Box(Modifier.height(32.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .background(MaterialTheme.colorScheme.surface),
                offset = DpOffset(
                    x = with(density) {
                        val tapXDp = tapOffsetX.toDp()
                        val estimatedMenuWidth = 200.dp
                        val parentDp = parentWidth.toDp()
                        val rightEdge = tapXDp + estimatedMenuWidth + 8.dp
                        if (rightEdge > parentDp) {
                            (parentDp - estimatedMenuWidth - 8.dp).coerceAtLeast(4.dp)
                        } else tapXDp
                    },
                    y = 0.dp
                ),
            ) {
                options.forEach { option ->
                    val isSelected = currentValue == option
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSelected(option); expanded = false },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent),
                        colors = MenuDefaults.itemColors(
                            textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun CapsuleTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit, tabs: List<String>) {
    val density = LocalDensity.current
    var tabWidths by remember { mutableStateOf(List(tabs.size) { 0f }) }

    val targetOffset = with(density) { tabWidths.take(selectedTab).sum().toDp() }
    val targetWidth = with(density) { (tabWidths.getOrNull(selectedTab) ?: 0f).toDp() }

    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "offset"
    )
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "width"
    )

    val contentWidth = with(density) { tabWidths.sum().toDp() }
    val squishLeft = animatedOffset.coerceAtLeast(0.dp)
    val squishRight = (animatedOffset + animatedWidth).coerceAtMost(contentWidth)
    val squishWidth = (squishRight - squishLeft).coerceAtLeast(0.dp)

    Surface(
        modifier = Modifier.wrapContentWidth().height(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = squishLeft)
                    .width(squishWidth)
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val textScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
                        label = "scale"
                    )
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                tabWidths = tabWidths.toMutableList().also {
                                    while (it.size <= index) it.add(0f)
                                    it[index] = coords.size.width.toFloat()
                                }
                            }
                            .height(32.dp).wrapContentWidth().clip(CircleShape)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTabSelected(index) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
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

@Composable
fun FilterChipDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    menuWidth: Dp = 150.dp
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = true, onClick = { expanded = true },
            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )
        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.width(menuWidth).background(MaterialTheme.colorScheme.surface),
                offset = DpOffset(0.dp, 4.dp)
            ) {
                options.forEach { (key, displayLabel) ->
                    val isSelected = selectedKey == key
                    DropdownMenuItem(
                        text = { Text(displayLabel, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onOptionSelected(key); expanded = false },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent),
                        colors = MenuDefaults.itemColors(textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSpinner(currentMode: String, onModeSelected: (String) -> Unit) {
    val modes = listOf("rule" to "规则模式", "global" to "全局模式", "direct" to "直连模式")
    FilterChipDropdown(
        label = modes.find { it.first == currentMode }?.second ?: currentMode,
        options = modes,
        selectedKey = currentMode,
        onOptionSelected = onModeSelected,
        menuWidth = 150.dp
    )
}

@Composable
fun LevelSpinner(currentLevel: String, onLevelSelected: (String) -> Unit) {
    val levels = listOf("info", "warning", "error", "debug", "silent")
    FilterChipDropdown(
        label = currentLevel.uppercase(),
        options = levels.map { it to it.uppercase() },
        selectedKey = currentLevel,
        onOptionSelected = onLevelSelected,
        menuWidth = 140.dp
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
                        withStyle(SpanStyle(color = if (isKey) keyColor else stringColor, fontWeight = if(isKey) FontWeight.Bold else FontWeight.Normal)) { append(str) }
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
                        withStyle(SpanStyle(color = if(isPunctuation) punctuationColor else Color.Unspecified)) { append(jsonStr[i]) }
                        i++
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
