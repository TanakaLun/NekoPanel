package io.tl.nekopanel.privileged

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class RootTrafficService : RootService() {
    private lateinit var bridge: RootNotificationBridge
    private lateinit var engine: TrafficMonitorEngine
    private lateinit var binder: PrivilegedTrafficBinder

    override fun onCreate() {
        bridge = RootNotificationBridge(applicationInfo.sourceDir)
        engine = TrafficMonitorEngine(bridge)
        binder = PrivilegedTrafficBinder(engine) { stopSelf() }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onUnbind(intent: Intent): Boolean = true

    override fun onDestroy() {
        if (::engine.isInitialized) engine.close()
        if (::bridge.isInitialized) bridge.close()
    }
}
