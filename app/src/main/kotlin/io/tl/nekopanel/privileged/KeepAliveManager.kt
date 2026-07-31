package io.tl.nekopanel.privileged

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

object KeepAliveManager {
    private const val SERVICE_VERSION = 1
    private const val SHELL_UID = 2000
    private const val ROOT_UID = 0
    private const val SHELL_PACKAGE = "com.android.shell"
    private const val APP_PACKAGE = "io.tl.nekopanel"
    private const val CHANNEL_ID = "traffic_monitor"
    private const val NOTIFICATION_ID = 114514

    private var generation = 0L
    private var service: IPrivilegedTrafficService? = null
    private var connection: ServiceConnection? = null
    private var desired: Request? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        val request = desired ?: return@OnBinderReceivedListener
        if (request.generation == generation) startShizuku(request)
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
    }

    fun isAvailable(): Boolean = Shizuku.pingBinder() && runCatching {
        Shizuku.getUid() in setOf(SHELL_UID, ROOT_UID) && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun start(
        context: Context,
        baseUrl: String,
        secret: String,
        notificationPriority: String,
        onResult: (Result<Unit>) -> Unit = {},
    ) {
        val appContext = context.applicationContext
        val uid = runCatching { Shizuku.getUid() }.getOrNull()
        if (!Shizuku.pingBinder() || uid !in setOf(SHELL_UID, ROOT_UID)) {
            onResult(Result.failure(IllegalStateException("Shizuku 未运行或必须以 ADB/Root 模式运行")))
            return
        }
        if (runCatching { Shizuku.checkSelfPermission() }.getOrNull() != PackageManager.PERMISSION_GRANTED) {
            onResult(Result.failure(SecurityException("Shizuku 尚未授权")))
            return
        }
        ensureNotificationChannel(appContext)
        if (uid == SHELL_UID) setNotificationDelegate(appContext, SHELL_PACKAGE)
        connection?.let { removeStaleConnection(it) }
        val request = Request(
            generation = ++generation,
            baseUrl = baseUrl,
            secret = secret,
            notificationPriority = notificationPriority,
            callback = onResult,
        )
        desired = request
        service = null
        startShizuku(request)
    }

    fun stop(context: Context) {
        generation++
        desired = null
        runCatching { service?.stopMonitoring() }
        service = null
        runCatching { Shizuku.unbindUserService(shizukuArgs(), connection, true) }
        connection = null
        setNotificationDelegate(context.applicationContext, null)
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    fun updateNotificationPriority(priority: String) {
        desired = desired?.copy(notificationPriority = priority)
        runCatching { service?.updateNotificationPriority(priority) }
    }

    private fun startShizuku(request: Request) {
        if (request.generation != generation) return
        if (!Shizuku.pingBinder()) {
            fail(request, IllegalStateException("Shizuku 未运行或 Binder 尚未就绪"))
            return
        }
        if (runCatching { Shizuku.getUid() }.getOrNull() !in setOf(SHELL_UID, ROOT_UID)) {
            fail(request, IllegalStateException("Shizuku 必须以 ADB 或 Root 模式运行"))
            return
        }
        if (runCatching { Shizuku.checkSelfPermission() }.getOrNull() != PackageManager.PERMISSION_GRANTED) {
            fail(request, SecurityException("Shizuku 尚未授权"))
            return
        }
        bind(request)
    }

    private fun bind(request: Request) {
        val requestConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                if (request.generation != generation) {
                    removeStaleConnection(this)
                    return
                }
                val remote = IPrivilegedTrafficService.Stub.asInterface(binder)
                runCatching {
                    remote.configure(request.baseUrl, request.secret, request.notificationPriority)
                    remote.startMonitoring()
                    service = remote
                    connection = this
                }.onSuccess {
                    request.callback(Result.success(Unit))
                }.onFailure {
                    fail(request, it)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                if (connection === this) {
                    service = null
                    connection = null
                }
            }
        }
        connection = requestConnection
        runCatching { Shizuku.bindUserService(shizukuArgs(), requestConnection) }
            .onFailure { fail(request, it) }
    }

    private fun fail(request: Request, throwable: Throwable) {
        if (request.generation != generation) return
        desired = null
        service = null
        connection = null
        generation++
        request.callback(Result.failure(throwable))
    }

    private fun removeStaleConnection(stale: ServiceConnection) {
        runCatching { Shizuku.unbindUserService(shizukuArgs(), stale, true) }
    }

    private fun shizukuArgs(): Shizuku.UserServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(APP_PACKAGE, KeepAliveTrafficService::class.java.name),
    )
        .daemon(true)
        .tag("nekopanel_keepalive_v1")
        .processNameSuffix("keepalive")
        .version(SERVICE_VERSION)

    private fun ensureNotificationChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "流量监控", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "显示实时流量信息"
                    setShowBadge(false)
                },
            )
        }
    }

    private fun setNotificationDelegate(context: Context, delegate: String?) {
        runCatching {
            context.getSystemService(NotificationManager::class.java).setNotificationDelegate(delegate)
        }
    }

    private data class Request(
        val generation: Long,
        val baseUrl: String,
        val secret: String,
        val notificationPriority: String,
        val callback: (Result<Unit>) -> Unit,
    )
}
