package io.tl.nekopanel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

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
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
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
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = alpha),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = alpha),
                    style = MiuixTheme.textStyles.footnote1,
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
    OverlayDropdownPreference(
        title = label,
        items = options,
        selectedIndex = options.indexOf(currentValue).coerceAtLeast(0),
        modifier = modifier,
        onSelectedIndexChange = { onSelected(options[it]) },
    )
}

@Composable
fun DropDownList(
    label: String,
    currentValue: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
    itemContent: @Composable (String, Boolean) -> Unit,
    displayValue: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        BasePreference(
            title = label,
            onClick = { expanded = true },
            trailing = {
                Box(Modifier.height(32.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = displayValue ?: currentValue,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            },
        )

        if (expanded) {
            WindowListPopup(
                show = expanded,
                onDismissRequest = { expanded = false },
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MiuixTheme.colorScheme.surface) {
                    Column(Modifier.padding(8.dp)) {
                        options.forEach { option ->
                            val isSelected = currentValue == option
                            Box(
                                modifier = Modifier.fillMaxWidth().clickable { onSelected(option); expanded = false }.padding(8.dp)
                            ) {
                                itemContent(option, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp), fontWeight = FontWeight.SemiBold, style = MiuixTheme.textStyles.subtitle, color = MiuixTheme.colorScheme.primary)
}

@Composable
fun SliderPreference(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..12f,
    steps: Int = 12,
) {
    BasePreference(
        title = label,
        description = "${value}dp",
        modifier = modifier,
        trailing = {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.width(160.dp)
            )
        }
    )
}