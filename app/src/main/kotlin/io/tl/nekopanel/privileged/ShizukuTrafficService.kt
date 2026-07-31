package io.tl.nekopanel.privileged

import android.content.Context
import android.system.Os
import androidx.annotation.Keep
import kotlin.system.exitProcess

@Keep
class ShizukuTrafficService() : IPrivilegedTrafficService.Stub() {
    private val publisher = ShellNotificationPublisher()
    private val engine = TrafficMonitorEngine(
        object : TrafficMonitorEngine.NotificationSink {
            override fun update(content: String) = publisher.update(content)
            override fun cancel() = publisher.cancel()
        },
    )

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    override fun configure(baseUrl: String, secret: String, notificationPriority: String) {
        engine.configure(baseUrl, secret, notificationPriority)
    }

    override fun startMonitoring() = engine.start()

    override fun updateNotificationPriority(notificationPriority: String) = engine.updatePriority(notificationPriority)

    override fun stopMonitoring() = engine.stop()

    override fun getUid(): Int = android.os.Process.myUid()

    override fun destroy() {
        engine.close()
        exitProcess(0)
    }

    companion object {
        private const val SHELL_UID = 2000

        init {
            if (Os.getuid() == 0) Os.setuid(SHELL_UID)
        }
    }
}
