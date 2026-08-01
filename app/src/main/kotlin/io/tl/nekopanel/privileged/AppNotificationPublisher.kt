package io.tl.nekopanel.privileged

import android.app.ActivityThread
import android.app.INotificationManager
import android.app.Notification
import android.app.PendingIntent
import android.content.AttributionSource
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.ServiceManager
import android.system.Os

internal class AppNotificationPublisher {
    private val context = AppContext()
    private val notificationManager = INotificationManager.Stub.asInterface(
        ServiceManager.getService(Context.NOTIFICATION_SERVICE),
    ) ?: error("notification service unavailable")

    private val opPackageName: String
        get() = when (Os.getuid()) {
            SHELL_UID -> SHELL_PACKAGE
            ROOT_UID -> "android"
            else -> error("App notification publisher requires UID $SHELL_UID or $ROOT_UID")
        }

    init {
        check(Os.getuid() == SHELL_UID || Os.getuid() == ROOT_UID) {
            "App notification publisher requires UID $SHELL_UID or $ROOT_UID"
        }
    }

    fun update(content: String) {
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(Icon.createWithResource(APP_PACKAGE, io.tl.nekopanel.R.drawable.ic_traffic_mono))
            .setContentTitle("NekoPanel 流量监控")
            .setContentText(content)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent().apply {
                        component = ComponentName(APP_PACKAGE, "$APP_PACKAGE.MainActivity")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        notificationManager.enqueueNotificationWithTag(
            APP_PACKAGE,
            opPackageName,
            null,
            NOTIFICATION_ID,
            notification,
            0,
        )
    }

    fun cancel() {
        notificationManager.cancelNotificationWithTag(
            APP_PACKAGE,
            opPackageName,
            null,
            NOTIFICATION_ID,
            0,
        )
    }

    private class AppContext : ContextWrapper(systemContext) {
        override fun getPackageName(): String = APP_PACKAGE
        override fun getOpPackageName(): String = APP_PACKAGE
        override fun getAttributionSource(): AttributionSource = AttributionSource.Builder(Os.getuid())
            .setPackageName(APP_PACKAGE)
            .build()
        override fun getApplicationContext(): Context = this
        override fun getDeviceId(): Int = 0

        companion object {
            private val systemContext: Context by lazy {
                val activityThread = ActivityThread.currentActivityThread()
                    ?: ActivityThread.systemMain()
                val getContext = activityThread.javaClass.methods.firstOrNull { method ->
                    method.name in listOf("getSystemContext", "getSystemUiContext") &&
                        method.parameterCount == 0 &&
                        Context::class.java.isAssignableFrom(method.returnType)
                } ?: error("ActivityThread lacks getSystemContext/getSystemUiContext")
                getContext.invoke(activityThread) as Context
            }
        }
    }

    private companion object {
        const val APP_PACKAGE = "io.tl.nekopanel"
        const val SHELL_PACKAGE = "com.android.shell"
        const val SHELL_UID = 2000
        const val ROOT_UID = 0
        const val CHANNEL_ID = "traffic_monitor"
        const val NOTIFICATION_ID = 114514
    }
}
