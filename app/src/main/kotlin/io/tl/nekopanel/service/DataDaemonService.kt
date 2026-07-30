package io.tl.nekopanel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
    private var connectionGeneration = 0L
    private var watchdogJob: Job? = null
    private var reconnectJob: Job? = null
    private var notificationPriority = "speed"

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
        notificationPriority = intent?.getStringExtra(EXTRA_NOTIFICATION_PRIORITY) ?: settings.notificationPriority
        val notification = buildNotification("正在连接...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (intent?.action == ACTION_REFRESH) {
            updateNotification()
            return START_STICKY
        }

        ApiClient.baseUrl = settings.apiBaseUrl
        ApiClient.secret = settings.apiSecret

        if (ApiClient.baseUrl.isNotBlank()) {
            startTrafficWebSocket()
        }

        return START_STICKY
    }

    @Synchronized
    private fun startTrafficWebSocket() {
        val generation = ++connectionGeneration
        updateNotification("正在连接...")
        trafficWs?.cancel()
        trafficWs = null
        watchdogJob?.cancel()
        reconnectJob?.cancel()
        releaseLocks()
        if (!isNetworkAvailable()) {
            updateNotification("等待网络连接...")
            registerNetworkCallback()
            return
        }

        unregisterNetworkCallback()
        acquireLocks()
        lastMessageTime = System.currentTimeMillis()
        scheduleWatchdog(generation)
        trafficWs = ApiClient.buildWebSocket(
            path = "/traffic",
            onText = { text ->
                if (generation != connectionGeneration) return@buildWebSocket
                lastMessageTime = System.currentTimeMillis()
                acquireLocks()
                scheduleWatchdog(generation)
                try {
                    val obj = JSONObject(text)
                    val d = obj.optLong("down", -1L)
                    val u = obj.optLong("up", -1L)
                    val dt = obj.optLong("downTotal", -1L)
                    val ut = obj.optLong("upTotal", -1L)
                    val dc = obj.optLong("downCumulative", -1L)
                    val uc = obj.optLong("upCumulative", -1L)
                    if (d >= 0 && u >= 0 && dt >= 0 && ut >= 0) {
                        globalDown = d; globalUp = u; totalDown = dt; totalUp = ut
                        settings.setTrafficSnapshot(d, u, dt, ut, dc, uc)
                        updateNotification()
                    }
                } catch (_: Exception) {}
            },
            onError = {
                if (generation != connectionGeneration) return@buildWebSocket
                updateNotification("连接中断，正在重连...")
                releaseLocks()
                scheduleReconnect(generation)
            },
        )
    }

    @Synchronized
    private fun scheduleWatchdog(generation: Long) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            delay(45_000)
            if (generation == connectionGeneration && System.currentTimeMillis() - lastMessageTime >= 45_000) {
                startTrafficWebSocket()
            }
        }
    }

    @Synchronized
    private fun scheduleReconnect(generation: Long) {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(2_000)
            if (generation == connectionGeneration) startTrafficWebSocket()
        }
    }

    private fun stopTrafficWebSocket() {
        trafficWs?.cancel()
        trafficWs = null
        connectionGeneration++
        watchdogJob?.cancel()
        reconnectJob?.cancel()
        releaseLocks()
        unregisterNetworkCallback()
    }

    private fun acquireLocks() {
        if (wakeLock?.isHeld != true) wakeLock?.acquire(60_000L)
        if (wifiLock?.isHeld != true) wifiLock?.acquire()
    }

    private fun releaseLocks() {
        if (wakeLock?.isHeld == true) runCatching { wakeLock?.release() }
        if (wifiLock?.isHeld == true) runCatching { wifiLock?.release() }
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

    private fun updateNotification(contentOverride: String? = null) {
        val speedLine = "↓ ${globalDown.formatSize()}/s  ↑ ${globalUp.formatSize()}/s"
        val totalLine = "累计 ↓ ${totalDown.formatSize()}  ↑ ${totalUp.formatSize()}"
        val content = contentOverride ?: if (notificationPriority == "total") totalLine else speedLine
        val notification = buildNotification(content)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val priority = notificationPriority
        val speedLine = "↓ ${globalDown.formatSize()}/s  ↑ ${globalUp.formatSize()}/s"
        val totalLine = "累计 ↓ ${totalDown.formatSize()}  ↑ ${totalUp.formatSize()}"
        val bigText = if (priority == "total") "$totalLine\n$speedLine" else "$speedLine\n$totalLine"
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
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "traffic_monitor"
        const val NOTIFICATION_ID = 114514
        const val ACTION_REFRESH = "io.tl.nekopanel.UPDATE_NOTIFICATION"
        private const val EXTRA_NOTIFICATION_PRIORITY = "notification_priority"

        fun start(context: Context) {
            val intent = Intent(context, DataDaemonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DataDaemonService::class.java))
        }

        fun refreshNotification(context: Context, priority: String) {
            val intent = Intent(context, DataDaemonService::class.java).apply {
                action = ACTION_REFRESH
                putExtra(EXTRA_NOTIFICATION_PRIORITY, priority)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
