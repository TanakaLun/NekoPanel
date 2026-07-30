package io.tl.nekopanel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

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
            minWidth = menuWidth,
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MiuixTheme.colorScheme.surface) {
                Column(Modifier.width(menuWidth).padding(8.dp)) {
                    options.forEach { (key, displayLabel) ->
                        val isSelected = selectedKey == key
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MiuixTheme.colorScheme.primaryContainer.copy(0.5f) else Color.Transparent)
                                .clickable { onOptionSelected(key); expanded = false }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                displayLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
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