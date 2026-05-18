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
import io.tl.nekopanel.MainActivity
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.util.formatSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.WebSocket
import org.json.JSONObject

class DataDaemonService : Service() {

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
                    val d = obj.optLong("down", -1L)
                    val u = obj.optLong("up", -1L)
                    val dt = obj.optLong("downTotal", -1L)
                    val ut = obj.optLong("upTotal", -1L)
                    if (d >= 0 && u >= 0 && dt >= 0 && ut >= 0) {
                        globalDown = d; globalUp = u; totalDown = dt; totalUp = ut
                        settings.accumulateTraffic(totalDown, totalUp)
                        updateNotification()
                    }
                } catch (_: Exception) {}
            },
            onError = { updateNotification("连接中断，等待重连...") }
        )
    }

    private fun stopTrafficWebSocket() {
        trafficWs?.cancel()
        trafficWs = null
    }

    private fun updateNotification(contentOverride: String? = null) {
        val content = contentOverride ?: "↓ ${globalDown.formatSize()}/s  ↑ ${globalUp.formatSize()}/s"
        val notification = buildNotification(content)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val totalText = "累计 ↓ ${totalDown.formatSize()}  ↑ ${totalUp.formatSize()}"
        val bigText = "$content\n$totalText"

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NekoPanel 流量监控")
            .setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW)
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "流量监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示实时流量信息"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTrafficWebSocket()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "traffic_monitor"
        const val NOTIFICATION_ID = 114514

        fun start(context: Context) {
            val intent = Intent(context, DataDaemonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DataDaemonService::class.java)
            context.stopService(intent)
        }
    }
}
