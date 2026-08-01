package io.tl.nekopanel.privileged

import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import kotlin.system.exitProcess

@Keep
class KeepAliveTrafficService() : IPrivilegedTrafficService.Stub() {
    private val publisher = AppNotificationPublisher()
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

    override fun getUid(): Int = Process.myUid()

    override fun destroy() {
        engine.close()
        exitProcess(0)
    }
}
