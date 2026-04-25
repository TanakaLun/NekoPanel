package io.tl.nekopanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.SettingsManager
import io.tl.nekopanel.ui.components.TypeBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun RulesScreen(refreshTick: Long, settings: SettingsManager) {
    var rules by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
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
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(refreshTick) { fetchRules() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rules, key = { it.optInt("index") }) { rule ->
                val type = rule.optString("type", "")
                val payload = rule.optString("payload", "")
                val proxy = rule.optString("proxy", "")
                val index = rule.optInt("index")
                val isDisabled = rule.optJSONObject("extra")?.optBoolean("disabled", false) ?: false
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                TypeBadge(type, settings.ruleBadgeStyle, settings.badgeCornerRadius, false)
                                Spacer(Modifier.width(8.dp))
                                Text(payload, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, lineHeight = 18.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("🎯 代理: $proxy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = !isDisabled, onCheckedChange = { isChecked ->
                            scope.launch(Dispatchers.IO) {
                                try { ApiClient.updateRulesDisable(mapOf(index.toString() to !isChecked)); fetchRules() } catch (_: Exception) {}
                            }
                        })
                    }
                }
            }
        }
    }
}