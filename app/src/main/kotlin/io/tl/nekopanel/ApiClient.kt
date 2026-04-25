package io.tl.nekopanel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

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

    suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = authorizedRequestBuilder().url("$baseUrl$path").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
            response.body?.string() ?: "{}"
        }
    }

    suspend fun request(method: String, path: String, body: String? = null) = withContext(Dispatchers.IO) {
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val reqBody = body?.let { RequestBody.create(mediaType, it) }
        val request = authorizedRequestBuilder().url("$baseUrl$path").method(method, reqBody).build()
        client.newCall(request).execute().use { }
    }

    suspend fun getConfigs(): JSONObject = JSONObject(get("/configs"))
    suspend fun updateConfigs(body: Map<String, Any>) = request("PATCH", "/configs", JSONObject(body).toString())
    suspend fun getProxies(): JSONObject = JSONObject(get("/proxies"))
    suspend fun updateProxy(group: String, name: String) = request("PUT", "/proxies/$group", JSONObject(mapOf("name" to name)).toString())
    suspend fun getRules(): JSONObject = JSONObject(get("/rules"))
    suspend fun updateRulesDisable(body: Map<String, Boolean>) = request("PATCH", "/rules/disable", JSONObject(body).toString())
    suspend fun getVersion(): JSONObject = JSONObject(get("/version"))

    fun buildWebSocket(path: String, onText: (String) -> Unit): WebSocket {
        val request = authorizedRequestBuilder().url("$baseUrl$path").build()
        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) = onText(text)
        })
    }
}
