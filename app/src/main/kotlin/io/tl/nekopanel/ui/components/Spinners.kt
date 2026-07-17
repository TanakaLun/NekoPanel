package io.tl.nekopanel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

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
