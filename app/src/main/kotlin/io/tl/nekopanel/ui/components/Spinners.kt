package io.tl.nekopanel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun FilterChipDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val entry = remember(options, selectedKey, onOptionSelected) {
        DropdownEntry(
            items = options.map { (key, displayLabel) ->
                DropdownItem(
                    text = displayLabel,
                    selected = key == selectedKey,
                    onClick = { onOptionSelected(key) },
                )
            },
        )
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MiuixTheme.colorScheme.primaryContainer.copy(0.5f),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.onPrimaryContainer)
            }
        }
        WindowListPopup(
            show = expanded,
            onDismissRequest = { expanded = false },
            onDismissFinished = {},
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                entry.items.forEachIndexed { index, item ->
                    DropdownImpl(
                        item = item,
                        optionSize = entry.items.size,
                        isSelected = item.selected,
                        index = index,
                        dropdownColors = DropdownDefaults.dropdownColors(),
                        isFirst = index == 0,
                        isLast = index == entry.items.lastIndex,
                        onSelectedIndexChange = {
                            item.onClick?.invoke()
                            dismiss?.invoke()
                        },
                    )
                }
            }
        }
    }
}

private val modeLabels = mapOf(
    "rule" to "规则模式",
    "global" to "全局模式",
    "direct" to "直连模式",
)

private val levelLabels = mapOf(
    "info" to "信息",
    "warning" to "警告",
    "error" to "错误",
    "debug" to "调试",
    "silent" to "静默",
)

@Composable
private fun LabeledSpinner(
    labels: Map<String, String>,
    current: String,
    values: List<String> = emptyList(),
    onOptionSelected: (String) -> Unit,
) {
    val keys = if (values.isNotEmpty()) values else labels.keys.toList()
    val options = keys.map { key -> key to (labels[key.lowercase()] ?: key) }
    val matched = options.firstOrNull { it.first.equals(current, ignoreCase = true) }
    FilterChipDropdown(
        label = matched?.second ?: current,
        options = options,
        selectedKey = matched?.first ?: current,
        onOptionSelected = onOptionSelected,
    )
}

@Composable
fun ModeSpinner(currentMode: String, modes: List<String>, onModeSelected: (String) -> Unit) {
    LabeledSpinner(labels = modeLabels, current = currentMode, values = modes, onOptionSelected = onModeSelected)
}

@Composable
fun LevelSpinner(currentLevel: String, onLevelSelected: (String) -> Unit) {
    LabeledSpinner(labels = levelLabels, current = currentLevel, onOptionSelected = onLevelSelected)
}
