package io.tl.nekopanel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.app.AlarmManager
import io.tl.nekopanel.MainActivity
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.network.ApiClient
import io.tl.nekopanel.util.formatSize
import kotlinx.coroutines.*
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
    private var lastMessageTime = System.currentTimeMillis()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        createNotificationChannel()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NekoPanel:WsWakeLock").apply { setReferenceCounted(false) }

        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "NekoPanel:WsWifiLock").apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("正在连接...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        ApiClient.baseUrl = settings.apiBaseUrl
        ApiClient.secret = settings.apiSecret

        if (ApiClient.baseUrl.isNotBlank()) {
            startTrafficWebSocket()
        }

        scheduleResurrection()
        return START_STICKY
    }

    private fun startTrafficWebSocket() {
        updateNotification("正在连接...")
        trafficWs?.cancel()
        if (!isNetworkAvailable()) {
            updateNotification("等待网络连接...")
            registerNetworkCallback()
            wakeLock?.release()
            wifiLock?.release()
            return
        }

        wakeLock?.acquire(24 * 60 * 60 * 1000L)
        wifiLock?.acquire()
        lastMessageTime = System.currentTimeMillis()

        scope.launch {
            // Watchdog: force reconnect if no data for 40s
            while (isActive) {
                delay(20000)
                if (System.currentTimeMillis() - lastMessageTime > 40000) {
                    trafficWs?.cancel()
                    startTrafficWebSocket()
                    break
                }
            }
        }

        scope.launch {
            trafficWs = ApiClient.buildWebSocket(
                path = "/traffic",
                onText = { text ->
                    lastMessageTime = System.currentTimeMillis()
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
                onError = {
                    updateNotification("连接中断，5秒后重连...")
                    wakeLock?.release()
                    wifiLock?.release()
                    if (isActive) scope.launch {
                        delay(5000)
                        startTrafficWebSocket()
                    }
                }
            )
        }
    }

    private fun stopTrafficWebSocket() {
        trafficWs?.cancel()
        trafficWs = null
        wakeLock?.release()
        wifiLock?.release()
        unregisterNetworkCallback()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback() {
        unregisterNetworkCallback()
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (trafficWs == null && ApiClient.baseUrl.isNotBlank()) startTrafficWebSocket()
            }
        }
        cm.registerNetworkCallback(NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try { (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        networkCallback = null
    }

    private fun scheduleResurrection() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DataDaemonService::class.java)
        val pi = PendingIntent.getService(this, 1999, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val triggerTime = SystemClock.elapsedRealtime() + 3 * 60 * 1000L
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
        }
    }

    private fun updateNotification(contentOverride: String? = null) {
        val content = contentOverride ?: "↓ ${globalDown.formatSize()}/s  ↑ ${globalUp.formatSize()}/s"
        val notification = buildNotification(content)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val totalText = "累计 ↓ ${totalDown.formatSize()}  ↑ ${totalUp.formatSize()}"
        val bigText = "$content\n$totalText"
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NekoPanel 流量监控")
            .setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setSmallIcon(io.tl.nekopanel.R.drawable.ic_traffic_mono)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "流量监控", NotificationManager.IMPORTANCE_LOW).apply {
                description = "显示实时流量信息"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTrafficWebSocket()
        scope.cancel()
        try { wakeLock?.release() } catch (_: Exception) {}
        try { wifiLock?.release() } catch (_: Exception) {}
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "traffic_monitor"
        const val NOTIFICATION_ID = 114514

        fun start(context: Context) {
            val intent = Intent(context, DataDaemonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DataDaemonService::class.java))
        }
    }
}
