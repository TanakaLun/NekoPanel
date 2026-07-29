package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.ui.components.TypeBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RulesScreen(refreshTick: Long, settings: SettingsManager) {
    var rules by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var disabledState by rememberSaveable { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    fun fetchRules() {
        scope.launch {
            isLoading = true
            try {
                val res = ApiClient.getRules()
                val arr = res.getJSONArray("rules")
                val list = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                rules = list
                disabledState = list.associate { it.optInt("index") to (it.optJSONObject("extra")?.optBoolean("disabled", false) ?: false) }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(refreshTick) { fetchRules() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(rules, key = { _, rule -> rule.optInt("index") }) { _, rule ->
                val type = rule.optString("type", "")
                val payload = rule.optString("payload", "")
                val proxy = rule.optString("proxy", "")
                val index = rule.optInt("index")
                val isDisabled = disabledState[index] ?: false
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                TypeBadge(type, settings.ruleBadgeStyle, settings.badgeCornerRadius, false)
                                Spacer(Modifier.width(8.dp))
                                Text(payload, fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body2, lineHeight = 18.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("🎯 代理: $proxy", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.outline)
                        }
                        Switch(checked = !isDisabled, onCheckedChange = { isChecked ->
                            disabledState = disabledState + (index to !isChecked)
                            scope.launch(Dispatchers.IO) {
                                try { ApiClient.updateRulesDisable(mapOf(index.toString() to !isChecked)) } catch (_: Exception) {}
                            }
                        })
                    }
                }
            }
        }
    }
}