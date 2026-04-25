package io.tl.nekopanel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

// ========================================================
// 修复报错的关键：添加数据模型定义。
// 这能让 MainActivity 和 TrafficScreen 识别 LogItem 和 ConnectionItem。
// ========================================================
data class LogItem(val type: String, val payload: String)

data class ConnectionItem(
    val id: String,
    val upload: Long,
    val download: Long,
    val start: String,
    val chains: List<String>,
    val rule: String,
    val rulePayload: String,
    val rawJson: JSONObject
)

object ApiClient {
    var baseUrl: String = ""
    var secret: String = ""

    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private fun authorizedRequestBuilder(): Request.Builder {
        val builder = Request.Builder()
        if (secret.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $secret")
        }
        return builder
    }

    // 通用 GET 请求
    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = authorizedRequestBuilder()
            .url("$baseUrl$path")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
            response.body?.string() ?: "{}"
        }
    }

    // 通用 PUT/PATCH/POST/DELETE 请求
    private suspend fun request(method: String, path: String, body: String? = null) = withContext(Dispatchers.IO) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val reqBody = body?.toRequestBody(mediaType)
        val request = authorizedRequestBuilder()
            .url("$baseUrl$path")
            .method(method, reqBody)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
        }
    }

    
    suspend fun getConfigs(): JSONObject = JSONObject(get("/configs"))
    
    suspend fun updateConfigs(body: Map<String, Any>) {
        request("PATCH", "/configs", JSONObject(body).toString())
    }
    
    suspend fun reloadConfigs(path: String = "") {
        request("PUT", "/configs", JSONObject(mapOf("path" to path)).toString())
    }

    suspend fun getProxies(): JSONObject = JSONObject(get("/proxies"))
    
    suspend fun updateProxy(group: String, name: String) {
        request("PUT", "/proxies/$group", JSONObject(mapOf("name" to name)).toString())
    }

    suspend fun getRules(): JSONObject = JSONObject(get("/rules"))
    
    suspend fun updateRulesDisable(body: Map<String, Boolean>) {
        request("PATCH", "/rules/disable", JSONObject(body).toString())
    }
    
    suspend fun restartCore() {
        request("POST", "/restart")
    }
    
    suspend fun getVersion(): JSONObject = JSONObject(get("/version"))
    
    suspend fun deleteAllConnections() {
        request("DELETE", "/connections")
    }
    
    suspend fun deleteConnection(id: String) {
        request("DELETE", "/connections/$id")
    }

    suspend fun getTraffic(): JSONObject = JSONObject(get("/traffic"))

    // --------------- WebSocket 监控 ---------------
    fun buildWebSocket(
        path: String,
        onText: (String) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): WebSocket {
        val request = authorizedRequestBuilder().url("$baseUrl$path").build()
        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                onText(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError(t)
            }
        })
    }
}
