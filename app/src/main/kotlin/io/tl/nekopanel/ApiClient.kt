package io.tl.nekopanel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    // 由外部注入 baseUrl 和 secret，从 SettingsManager 读取
    var baseUrl: String = ""
    var secret: String = ""

    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private fun buildAuthHeader(): Headers {
        return if (secret.isNotBlank()) {
            Headers.Builder().add("Authorization", "Bearer $secret").build()
        } else {
            Headers.headersOf()
        }
    }

    // 通用 GET 请求并返回 JSONObject
    private suspend fun getJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl$path").headers(buildAuthHeader()).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        }
    }

    // 通用 PUT/PATCH 请求
    private suspend fun request(
        method: String,
        path: String,
        bodyJson: String? = null
    ): Unit = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url("$baseUrl$path").headers(buildAuthHeader())
        when (method.uppercase()) {
            "PUT" -> builder.put(bodyJson?.toRequestBody("application/json".toMediaType()) ?: RequestBody.create(null, ""))
            "PATCH" -> builder.patch(bodyJson?.toRequestBody("application/json".toMediaType()) ?: RequestBody.create(null, ""))
            "DELETE" -> builder.delete()
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("${response.code}: ${response.message}")
        }
    }

    // --------------- 接口方法 ---------------
    suspend fun getProxies(): JSONObject = getJson("/proxies")
    suspend fun updateProxy(name: String, body: Map<String, String>): Unit {
        val json = JSONObject(body)
        request("PUT", "/proxies/$name", json.toString())
    }
    suspend fun getProxyDelay(name: String, url: String, timeout: Int): JSONObject =
        getJson("/proxies/$name/delay?url=${java.net.URLEncoder.encode(url, "utf-8")}&timeout=$timeout")

    suspend fun getGroupDelay(name: String, url: String, timeout: Int): JSONObject =
        getJson("/group/$name/delay?url=${java.net.URLEncoder.encode(url, "utf-8")}&timeout=$timeout")

    suspend fun getConfigs(): JSONObject = getJson("/configs")
    suspend fun updateConfigs(body: Map<String, Any>): Unit {
        val json = JSONObject(body)
        request("PATCH", "/configs", json.toString())
    }
    suspend fun reloadConfigs(path: String = ""): Unit {
        val json = JSONObject(mapOf("path" to path))
        request("PUT", "/configs", json.toString())
    }

    suspend fun getRules(): JSONObject = getJson("/rules")
    suspend fun updateRulesDisable(body: Map<String, Boolean>): Unit {
        val json = JSONObject(body)
        request("PATCH", "/rules/disable", json.toString())
    }
    suspend fun restartCore(): Unit = request("POST", "/restart")
    suspend fun getVersion(): JSONObject = getJson("/version")
    suspend fun deleteAllConnections(): Unit = request("DELETE", "/connections")
    suspend fun deleteConnection(id: String): Unit = request("DELETE", "/connections/$id")

    // --------------- WebSocket 创建 ---------------
    fun buildWebSocket(
        path: String,
        onText: (String) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): WebSocket {
        val url = "$baseUrl$path"
        val request = Request.Builder().url(url).headers(buildAuthHeader()).build()
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