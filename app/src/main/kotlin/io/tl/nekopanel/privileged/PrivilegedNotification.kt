package io.tl.nekopanel.privileged

import android.app.INotificationManager
import android.content.Context
import android.os.ServiceManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

internal object PrivilegedNotification {
    const val SHELL_PACKAGE = "com.android.shell"
    const val SHELL_UID = 2000
    const val TAG = "nekopanel_privileged_traffic"
    const val NOTIFICATION_ID = 114515
    const val CHANNEL_ID = "nekopanel_privileged_traffic"

    fun cancelShellNotification() {
        if (!Shizuku.pingBinder()) return
        runCatching {
            val notificationManager = INotificationManager.Stub.asInterface(
                ShizukuBinderWrapper(ServiceManager.getService(Context.NOTIFICATION_SERVICE)),
            )
            notificationManager.cancelNotificationWithTag(
                SHELL_PACKAGE,
                SHELL_PACKAGE,
                TAG,
                NOTIFICATION_ID,
                0,
            )
        }
    }
}
