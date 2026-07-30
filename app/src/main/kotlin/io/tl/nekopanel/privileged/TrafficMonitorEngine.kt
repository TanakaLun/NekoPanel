package io.tl.nekopanel.privileged

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

internal class TrafficMonitorEngine(
    private val notificationSink: NotificationSink,
) : AutoCloseable {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "NekoPanel-PrivilegedTraffic").apply { isDaemon = true }
    }
    private val generation = AtomicLong()
    private var reconnect: ScheduledFuture<*>? = null
    private var socket: WebSocket? = null
    private var baseUrl = ""
    private var secret = ""
    private var priority = "speed"
    private var running = false
    private var retryAttempt = 0

    @Synchronized
    fun configure(baseUrl: String, secret: String, priority: String) {
        val endpointChanged = this.baseUrl != baseUrl || this.secret != secret
        this.baseUrl = baseUrl.trimEnd('/')
        this.secret = secret
        this.priority = priority
        if (running && endpointChanged) connect()
    }

    @Synchronized
    fun start() {
        if (running) return
        running = true
        retryAttempt = 0
        notificationSink.update("正在连接...")
        connect()
    }

    @Synchronized
    fun updatePriority(priority: String) {
        this.priority = priority
    }

    @Synchronized
    fun stop() {
        running = false
        generation.incrementAndGet()
        reconnect?.cancel(false)
        reconnect = null
        socket?.cancel()
        socket = null
        notificationSink.cancel()
    }

    @Synchronized
    private fun connect() {
        val currentGeneration = generation.incrementAndGet()
        reconnect?.cancel(false)
        reconnect = null
        socket?.cancel()
        socket = null
        if (!running || baseUrl.isBlank()) {
            notificationSink.update("等待 API 配置...")
            return
        }
        val headers = if (secret.isBlank()) Headers.headersOf() else Headers.headersOf("Authorization", "Bearer $secret")
        val request = runCatching { Request.Builder().url("$baseUrl/traffic").headers(headers).build() }
            .getOrElse {
                notificationSink.update("API 地址无效")
                scheduleReconnect(currentGeneration)
                return
            }
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                synchronized(this@TrafficMonitorEngine) {
                    if (currentGeneration == generation.get()) retryAttempt = 0
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (currentGeneration != generation.get()) return
                runCatching {
                    val data = JSONObject(text)
                    val speed = "↓ ${formatSize(data.optLong("down"))}/s  ↑ ${formatSize(data.optLong("up"))}/s"
                    val total = "累计 ↓ ${formatSize(data.optLong("downTotal"))}  ↑ ${formatSize(data.optLong("upTotal"))}"
                    notificationSink.update(if (priority == "total") total else speed)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnect(currentGeneration)
            }

            override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
                notificationSink.update("连接中断，等待重连...")
                scheduleReconnect(currentGeneration)
            }
        })
    }

    @Synchronized
    private fun scheduleReconnect(failedGeneration: Long) {
        if (!running || failedGeneration != generation.get() || reconnect?.isDone == false) return
        val delaySeconds = (1L shl retryAttempt.coerceAtMost(5)).coerceAtMost(30L)
        retryAttempt++
        reconnect = scheduler.schedule({ synchronized(this) { if (running) connect() } }, delaySeconds, TimeUnit.SECONDS)
    }

    override fun close() {
        stop()
        scheduler.shutdownNow()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    interface NotificationSink {
        fun update(content: String)
        fun cancel()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = -1
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return String.format("%.1f %s", value, units[unit])
    }
}
