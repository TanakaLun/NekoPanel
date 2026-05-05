package io.tl.nekopanel.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CapsuleTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit, tabs: List<String>) {
    Surface(
        modifier = Modifier.wrapContentWidth().height(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val textScale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")
                    Box(
                        modifier = Modifier.height(32.dp).wrapContentWidth().clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
fun ModeSpinner(currentMode: String, onModeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("rule" to "规则模式", "global" to "全局模式", "direct" to "直连模式")

    Box {
        FilterChip(
            selected = true, onClick = { expanded = true },
            label = { Text(modes.find { it.first == currentMode }?.second ?: currentMode, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )
        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.width(150.dp).background(MaterialTheme.colorScheme.surface),
                offset = DpOffset(0.dp, 4.dp)
            ) {
                modes.forEach { (key, label) ->
                    val isSelected = currentMode == key
                    DropdownMenuItem(
                        text = { Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        onClick = { onModeSelected(key); expanded = false },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        colors = MenuDefaults.itemColors(textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
fun LevelSpinner(currentLevel: String, onLevelSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val levels = listOf("info", "warning", "error", "debug", "silent")

    Box {
        FilterChip(
            selected = true, onClick = { expanded = true },
            label = { Text(currentLevel.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )
        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(140.dp)) {
                levels.forEach { level ->
                    val isSelected = currentLevel == level
                    DropdownMenuItem(
                        text = { Text(level.uppercase(), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onLevelSelected(level); expanded = false },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                        colors = MenuDefaults.itemColors(textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDropdownMenuInline(label: String, currentValue: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

        Box(modifier = Modifier.width(160.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = currentValue, onValueChange = {}, readOnly = true,
                    textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
                MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(18.dp))) {
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        options.forEach { option ->
                            val isSelected = currentValue == option
                            DropdownMenuItem(
                                text = { Text(option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { onSelected(option); expanded = false },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent),
                                colors = MenuDefaults.itemColors(textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
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
