package io.tl.nekopanel.service

import android.content.Context
import android.util.Log

object RootDaemonManager {
    private const val TAG = "RootDaemon"

    val running: Boolean get() = NekoDaemon.connected
    val nativeAvailable: Boolean get() = NekoDaemon.loadNative()
    val uptime: Int get() = NekoDaemon.uptime

    fun start(context: Context) {
        if (NekoDaemon.start(context)) {
            Log.i(TAG, "Daemon started via native")
        } else if (Shell.checkRootAccess()) {
            Log.i(TAG, "Native unavailable, falling back to shell daemon")
            fallbackShellDaemon(context)
        } else {
            Log.w(TAG, "No root access available")
        }
    }

    fun stop() {
        NekoDaemon.stop()
        Shell.execute("kill -9 \$(pgrep -f nekopanel/daemon.sh) 2>/dev/null; rm -f /data/local/tmp/nekopanel/daemon.sh")
    }

    private fun fallbackShellDaemon(context: Context) {
        val pkg = context.packageName
        val svc = DataDaemonService::class.java.name
        val cmd = buildString {
            appendLine("#!/system/bin/sh")
            appendLine("while true; do")
            appendLine("  am start-foreground-service -n $pkg/$svc >/dev/null 2>&1")
            appendLine("  sleep 30")
            appendLine("done")
        }
        Shell.execute("mkdir -p /data/local/tmp/nekopanel && " +
            "echo '${cmd.replace("'", "'\\''")}' > /data/local/tmp/nekopanel/daemon.sh && " +
            "chmod 755 /data/local/tmp/nekopanel/daemon.sh && " +
            "nohup sh /data/local/tmp/nekopanel/daemon.sh >/dev/null 2>&1 &")
    }
}
