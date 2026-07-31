package io.tl.nekopanel.privileged

import android.app.ActivityThread
import android.app.INotificationManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.AttributionSource
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ParceledListSlice
import android.graphics.drawable.Icon
import android.os.ServiceManager
import android.system.Os

internal class ShellNotificationPublisher {
    private val context = ShellContext()
    private val notificationManager = INotificationManager.Stub.asInterface(
        ServiceManager.getService(Context.NOTIFICATION_SERVICE),
    ) ?: error("notification service unavailable")

    init {
        check(Os.getuid() == PrivilegedNotification.SHELL_UID) { "Shell notification publisher requires UID ${PrivilegedNotification.SHELL_UID}" }
        notificationManager.createNotificationChannelsForPackage(
            PrivilegedNotification.SHELL_PACKAGE,
            PrivilegedNotification.SHELL_UID,
            ParceledListSlice(
                listOf(
                    NotificationChannel(PrivilegedNotification.CHANNEL_ID, "NekoPanel", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "NekoPanel 实时流量"
                        enableLights(false)
                        enableVibration(false)
                        setShowBadge(false)
                    },
                ),
            ),
        )
    }

    fun update(content: String) {
        val notification = Notification.Builder(context, PrivilegedNotification.CHANNEL_ID)
            .setSmallIcon(Icon.createWithResource("android", android.R.drawable.stat_notify_sync))
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
            PrivilegedNotification.SHELL_PACKAGE,
            PrivilegedNotification.SHELL_PACKAGE,
            PrivilegedNotification.TAG,
            PrivilegedNotification.NOTIFICATION_ID,
            notification,
            0,
        )
    }

    fun cancel() {
        notificationManager.cancelNotificationWithTag(
            PrivilegedNotification.SHELL_PACKAGE,
            PrivilegedNotification.SHELL_PACKAGE,
            PrivilegedNotification.TAG,
            PrivilegedNotification.NOTIFICATION_ID,
            0,
        )
    }

    private class ShellContext : ContextWrapper(systemContext) {
        override fun getPackageName(): String = PrivilegedNotification.SHELL_PACKAGE
        override fun getOpPackageName(): String = PrivilegedNotification.SHELL_PACKAGE
        override fun getAttributionSource(): AttributionSource = AttributionSource.Builder(Os.getuid())
            .setPackageName(PrivilegedNotification.SHELL_PACKAGE)
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
    }
}
