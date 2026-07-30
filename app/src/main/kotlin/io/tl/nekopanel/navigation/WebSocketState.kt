package io.tl.nekopanel.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.model.ConnectionItem
import io.tl.nekopanel.model.LogItem
import io.tl.nekopanel.network.ApiClient
import org.json.JSONObject

@Stable
class WebSocketState(
    settings: SettingsManager,
    apiBaseUrl: String,
    apiSecret: String,
    logLevel: String,
) {
    var globalInUse by mutableLongStateOf(0L)
        private set
    var globalDown by mutableLongStateOf(0L)
        private set
    var globalUp by mutableLongStateOf(0L)
        private set
    var totalDown by mutableLongStateOf(0L)
        private set
    var totalUp by mutableLongStateOf(0L)
        private set
    var connections by mutableStateOf<List<ConnectionItem>>(emptyList())
        private set
    val logs = mutableStateListOf<LogItem>()

    private val memWs = ApiClient.buildWebSocket("/memory", onText = { text ->
        try { globalInUse = JSONObject(text).optLong("inuse", 0L) } catch (_: Exception) {}
    })
    private val trafficWs = ApiClient.buildWebSocket("/traffic", onText = { text ->
        try {
            val obj = JSONObject(text)
            globalDown = obj.optLong("down", 0L)
            globalUp = obj.optLong("up", 0L)
            totalDown = obj.optLong("downTotal", 0L)
            totalUp = obj.optLong("upTotal", 0L)
            settings.setTrafficSnapshot(
                obj.optLong("down", 0L), obj.optLong("up", 0L),
                obj.optLong("downTotal", 0L), obj.optLong("upTotal", 0L),
                obj.optLong("downCumulative", -1L), obj.optLong("upCumulative", -1L),
            )
        } catch (_: Exception) {}
    })
    private val logsWs = ApiClient.buildWebSocket("/logs?level=$logLevel", onText = { text ->
        try {
            val obj = JSONObject(text)
            logs.add(LogItem(obj.optString("type", ""), obj.optString("payload", "")))
            if (logs.size > 1000) logs.removeAt(0)
        } catch (_: Exception) {}
    })
    private var connWs: okhttp3.WebSocket? = null

    fun removeConnection(id: String) {
        connections = connections.filter { it.id != id }
    }

    fun clearConnections() {
        connections = emptyList()
    }

    fun startConnections(selectedTab: Int) {
        connWs?.cancel()
        connWs = null
        if (selectedTab != 2) return
        connWs = ApiClient.buildWebSocket("/connections?interval=1000", onText = { text ->
            try {
                val arr = JSONObject(text).getJSONArray("connections")
                val list = mutableListOf<ConnectionItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val meta = obj.getJSONObject("metadata")
                    val chains = obj.getJSONArray("chains")
                    val proxy = if (chains.length() > 0) chains.getString(chains.length() - 1) else "Direct"
                    list.add(ConnectionItem(
                        id = obj.getString("id"),
                        host = meta.optString("host").ifBlank { meta.optString("destinationIP") },
                        network = meta.optString("network"),
                        proxy = proxy,
                        upload = obj.optLong("upload", 0L),
                        download = obj.optLong("download", 0L),
                        rawJson = obj.toString(),
                    ))
                }
                connections = list
            } catch (_: Exception) {}
        })
    }

    fun stopConnections() {
        connWs?.cancel()
        connWs = null
    }

    fun cancel() {
        memWs.cancel()
        trafficWs.cancel()
        logsWs.cancel()
        connWs?.cancel()
    }
}

@Composable
fun rememberWebSocketState(
    settings: SettingsManager,
    apiBaseUrl: String,
    apiSecret: String,
    logLevel: String,
    selectedTab: Int,
): WebSocketState {
    val state = remember(apiBaseUrl, apiSecret) {
        WebSocketState(settings, apiBaseUrl, apiSecret, logLevel)
    }

    LaunchedEffect(selectedTab) {
        state.startConnections(selectedTab)
    }

    return state
}
