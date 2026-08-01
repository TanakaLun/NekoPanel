package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.model.RuleInfo
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.TypeBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun RulesScreen(
    refreshTick: Long,
    settings: SettingsManager,
    scaffoldPadding: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    var rules by remember { mutableStateOf<List<RuleInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var disabledState by rememberSaveable { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    fun fetchRules() {
        scope.launch {
            isLoading = true
            try {
                val list = ApiClient.getRules()
                rules = list
                disabledState = list.associate { it.id to it.disabled }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(refreshTick) { fetchRules() }

    if (isLoading) {
        Box(Modifier.fillMaxSize().padding(scaffoldPadding), Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(
            modifier = Modifier.scrollEndHaptic(),
            contentPadding = PaddingValues(
                start = scaffoldPadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = scaffoldPadding.calculateTopPadding() + 16.dp,
                end = scaffoldPadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(rules, key = { _, rule -> rule.id }) { _, rule ->
                val isDisabled = disabledState[rule.id] ?: rule.disabled
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                TypeBadge(rule.type, settings.ruleBadgeStyle, settings.badgeCornerRadius, false)
                                Spacer(Modifier.width(8.dp))
                                Text(rule.payload, fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body2, lineHeight = 18.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("🎯 代理: ${rule.proxy}", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                        }
                        Switch(checked = !isDisabled, onCheckedChange = { isChecked ->
                            val prevDisabled = isDisabled
                            val targetDisabled = !isChecked
                            disabledState = disabledState + (rule.id to targetDisabled)
                            scope.launch(Dispatchers.IO) {
                                try { ApiClient.updateRuleDisabled(rule.id, prevDisabled, targetDisabled) } catch (_: Exception) {}
                            }
                        })
                    }
                }
            }
        }
    }
}
