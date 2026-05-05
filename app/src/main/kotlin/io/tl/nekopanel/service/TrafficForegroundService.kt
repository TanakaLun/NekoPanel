package io.tl.nekopanel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import io.tl.nekopanel.ApiClient
import io.tl.nekopanel.MainActivity
import io.tl.nekopanel.SettingsManager
import io.tl.nekopanel.ui.components.formatSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.WebSocket
import org.json.JSONObject

class TrafficForegroundService : Service() {

    private var trafficWs: WebSocket? = null
    private lateinit var settings: SettingsManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var globalDown = 0L
    private var globalUp = 0L
    private var totalDown = 0L
    private var totalUp = 0L

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("正在连接...")
        startForeground(NOTIFICATION_ID, notification)

        ApiClient.baseUrl = settings.apiBaseUrl
        ApiClient.secret = settings.apiSecret

        if (ApiClient.baseUrl.isNotBlank()) {
            startTrafficWebSocket()
        }

        return START_STICKY
    }

    private fun startTrafficWebSocket() {
        trafficWs?.cancel()
        trafficWs = ApiClient.buildWebSocket(
            path = "/traffic",
            onText = { text ->
                try {
                    val obj = JSONObject(text)
                    globalDown = obj.optLong("down", 0L)
                    globalUp = obj.optLong("up", 0L)
                    totalDown = obj.optLong("downTotal", 0L)
                    totalUp = obj.optLong("upTotal", 0L)
                    settings.accumulateTraffic(totalDown, totalUp)
                    updateNotification()
                } catch (_: Exception) {}
            },
            onError = {
                updateNotification("连接中断，等待重连...")
            }
        )
    }

    private fun updateNotification(contentOverride: String? = null) {
        val content = contentOverride ?: "↓ ${globalDown.formatSize()}/s  ↑ ${globalUp.formatSize()}/s"
        val notification = buildNotification(content)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val totalText = "累计 ↓ ${totalDown.formatSize()}  ↑ ${totalUp.formatSize()}"

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NekoPanel 流量监控")
            .setContentText(content)
            .setSubText(totalText)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "流量监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示实时流量信息和累计统计"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        trafficWs?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "traffic_monitor"
        const val NOTIFICATION_ID = 114514

        fun start(context: Context) {
            val intent = Intent(context, TrafficForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrafficForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
