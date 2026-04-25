package io.tl.nekopanel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    var baseUrl: String = ""
    var secret: String = ""

    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private fun buildHeaders() = if (secret.isNotBlank())
        Headers.Builder().add("Authorization", "Bearer $secret").build()
    else Headers.headersOf()

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl$path").headers(buildHeaders()).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
            response.body?.string() ?: "{}"
        }
    }

    private suspend fun request(method: String, path: String, bodyJson: String? = null): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url("$baseUrl$path").headers(buildHeaders())
        val body = bodyJson?.toRequestBody("application/json".toMediaType())
        when (method.uppercase()) {
            "PUT" -> builder.put(body ?: RequestBody.create(null, ""))
            "PATCH" -> builder.patch(body ?: RequestBody.create(null, ""))
            "POST" -> builder.post(body ?: RequestBody.create(null, ""))
            "DELETE" -> builder.delete()
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
            response.body?.string() ?: ""
        }
    }

    suspend fun getProxies(): JSONObject = JSONObject(get("/proxies"))
    suspend fun updateProxy(name: String, body: Map<String, String>) {
        request("PUT", "/proxies/$name", JSONObject(body).toString())
    }
    suspend fun getProxyDelay(name: String, url: String, timeout: Int): JSONObject {
        val encoded = java.net.URLEncoder.encode(url, "utf-8")
        return JSONObject(get("/proxies/$name/delay?url=$encoded&timeout=$timeout"))
    }
    suspend fun getGroupDelay(name: String, url: String, timeout: Int): JSONObject {
        val encoded = java.net.URLEncoder.encode(url, "utf-8")
        return JSONObject(get("/group/$name/delay?url=$encoded&timeout=$timeout"))
    }
    suspend fun getConfigs(): JSONObject = JSONObject(get("/configs"))
    suspend fun updateConfigs(body: Map<String, Any>) {
        request("PATCH", "/configs", JSONObject(body).toString())
    }
    suspend fun reloadConfigs(path: String = "") {
        request("PUT", "/configs", JSONObject(mapOf("path" to path)).toString())
    }
    suspend fun getRules(): JSONObject = JSONObject(get("/rules"))
    suspend fun updateRulesDisable(body: Map<String, Boolean>) {
        request("PATCH", "/rules/disable", JSONObject(body).toString())
    }
    suspend fun restartCore() { request("POST", "/restart") }
    suspend fun getVersion(): JSONObject = JSONObject(get("/version"))
    suspend fun deleteAllConnections() { request("DELETE", "/connections") }
    suspend fun deleteConnection(id: String) { request("DELETE", "/connections/$id") }

    fun buildWebSocket(
        path: String,
        onText: (String) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): WebSocket {
        val builder = Request.Builder().url("$baseUrl$path")
        if (secret.isNotBlank()) builder.addHeader("Authorization", "Bearer $secret")
        return client.newWebSocket(builder.build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { onText(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { onError(t) }
        })
    }
}