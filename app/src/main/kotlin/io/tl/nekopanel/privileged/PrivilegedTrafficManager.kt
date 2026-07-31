package io.tl.nekopanel.privileged

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import rikka.shizuku.Shizuku

object PrivilegedTrafficManager {
    private const val SHIZUKU_PERMISSION_REQUEST = 4101
    private const val SERVICE_VERSION = 2
    private const val SHELL_UID = 2000
    private const val ROOT_UID = 0

    data class Capabilities(
        val shizuku: Boolean,
        val root: Boolean,
    ) {
        fun supports(backend: PrivilegedBackendType): Boolean = when (backend) {
            PrivilegedBackendType.Shizuku -> shizuku
            PrivilegedBackendType.Root -> root
        }
    }

    private var generation = 0L
    private var service: IPrivilegedTrafficService? = null
    private var connection: ServiceConnection? = null
    private var desired: Request? = null
    private var permissionRequest: PermissionRequest? = null

    private val permissionListener: Shizuku.OnRequestPermissionResultListener by lazy {
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != SHIZUKU_PERMISSION_REQUEST) return@OnRequestPermissionResultListener
        val request = permissionRequest ?: return@OnRequestPermissionResultListener
        permissionRequest = null
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        if (request.generation != generation) return@OnRequestPermissionResultListener
        request.callback(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        val request = desired ?: return@OnBinderReceivedListener
        if (request.backend == PrivilegedBackendType.Shizuku && request.generation == generation) {
            startShizuku(request)
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
    }

    fun capabilities(): Capabilities = Capabilities(shizukuAvailable(), Shell.isAppGrantedRoot() == true)

    fun probeCapabilities(onResult: (Capabilities) -> Unit) {
        Shell.getShell { shell -> onResult(Capabilities(shizukuAvailable(), shell.isRoot)) }
    }

    fun requestShizukuPermission(onResult: (Boolean) -> Unit) {
        if (!Shizuku.pingBinder() || runCatching { Shizuku.getUid() }.getOrNull() !in setOf(SHELL_UID, ROOT_UID)) {
            onResult(false)
            return
        }
        if (runCatching { Shizuku.checkSelfPermission() }.getOrNull() == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        val requestGeneration = ++generation
        permissionRequest = PermissionRequest(requestGeneration, onResult)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        runCatching { Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST) }
            .onFailure {
                permissionRequest = null
                Shizuku.removeRequestPermissionResultListener(permissionListener)
                onResult(false)
            }
    }

    fun requestRootAccess(onResult: (Boolean) -> Unit) {
        Shell.getShell { shell -> onResult(shell.isRoot) }
    }

    fun start(
        context: Context,
        backend: PrivilegedBackendType,
        baseUrl: String,
        secret: String,
        notificationPriority: String,
        onResult: (Result<Unit>) -> Unit = {},
    ) {
        desired?.let { previous -> connection?.let { removeStaleConnection(previous.backend, it) } }
        val request = Request(
            generation = ++generation,
            context = context.applicationContext,
            backend = backend,
            baseUrl = baseUrl,
            secret = secret,
            notificationPriority = notificationPriority,
            callback = onResult,
        )
        desired = request
        service = null
        when (backend) {
            PrivilegedBackendType.Shizuku -> startShizuku(request)
            PrivilegedBackendType.Root -> {
                Shell.getShell { shell ->
                    if (request.generation != generation) return@getShell
                    if (shell.isRoot) bind(request) else fail(request, SecurityException("Root 权限不可用"))
                }
            }
        }
    }

    fun updateNotificationPriority(priority: String) {
        desired = desired?.copy(notificationPriority = priority)
        runCatching { service?.updateNotificationPriority(priority) }
    }

    fun stop(context: Context, backend: PrivilegedBackendType) {
        generation++
        desired = null
        permissionRequest = null
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        runCatching { service?.stopMonitoring() }
        service = null
        when (backend) {
            PrivilegedBackendType.Shizuku -> runCatching {
                Shizuku.unbindUserService(shizukuArgs(), connection, true)
            }
            PrivilegedBackendType.Root -> RootService.stop(rootIntent(context.applicationContext))
        }
        connection = null
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
                if (request.generation != generation || desired?.backend != request.backend) {
                    removeStaleConnection(request.backend, this)
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
        runCatching {
            when (request.backend) {
                PrivilegedBackendType.Shizuku -> Shizuku.bindUserService(shizukuArgs(), requestConnection)
                PrivilegedBackendType.Root -> RootService.bind(rootIntent(request.context), requestConnection)
            }
        }.onFailure { fail(request, it) }
    }

    private fun fail(request: Request, throwable: Throwable) {
        if (request.generation != generation) return
        desired = null
        service = null
        connection = null
        generation++
        request.callback(Result.failure(throwable))
    }

    private fun shizukuAvailable(): Boolean = Shizuku.pingBinder() && runCatching {
        Shizuku.getUid() in setOf(SHELL_UID, ROOT_UID) && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    private fun removeStaleConnection(backend: PrivilegedBackendType, stale: ServiceConnection) {
        when (backend) {
            PrivilegedBackendType.Shizuku -> runCatching { Shizuku.unbindUserService(shizukuArgs(), stale, true) }
            PrivilegedBackendType.Root -> runCatching { RootService.unbind(stale) }
        }
    }

    private fun shizukuArgs(): Shizuku.UserServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("io.tl.nekopanel", ShizukuTrafficService::class.java.name),
    )
        .daemon(true)
        .tag("nekopanel_traffic_v1")
        .processNameSuffix("traffic")
        .version(SERVICE_VERSION)

    private fun rootIntent(context: Context): Intent = Intent(context, RootTrafficService::class.java)
        .addCategory(RootService.CATEGORY_DAEMON_MODE)

    private data class PermissionRequest(
        val generation: Long,
        val callback: (Boolean) -> Unit,
    )

    private data class Request(
        val generation: Long,
        val context: Context,
        val backend: PrivilegedBackendType,
        val baseUrl: String,
        val secret: String,
        val notificationPriority: String,
        val callback: (Result<Unit>) -> Unit,
    )
}
