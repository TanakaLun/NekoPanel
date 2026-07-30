package io.tl.nekopanel.privileged

import android.os.Process

internal class PrivilegedTrafficBinder(
    private val engine: TrafficMonitorEngine,
    private val onDestroy: () -> Unit,
) : IPrivilegedTrafficService.Stub() {
    override fun configure(baseUrl: String, secret: String, notificationPriority: String) {
        engine.configure(baseUrl, secret, notificationPriority)
    }

    override fun startMonitoring() {
        engine.start()
    }

    override fun updateNotificationPriority(notificationPriority: String) {
        engine.updatePriority(notificationPriority)
    }

    override fun stopMonitoring() {
        engine.stop()
    }

    override fun getUid(): Int = Process.myUid()

    override fun destroy() {
        engine.close()
        onDestroy()
    }
}
