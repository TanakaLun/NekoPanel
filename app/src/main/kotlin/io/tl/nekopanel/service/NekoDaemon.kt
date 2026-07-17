package io.tl.nekopanel.service

import android.content.Context
import android.os.IBinder
import android.util.Log
import io.tl.nekopanel.server.INekoDaemon

object NekoDaemon {
    private const val TAG = "NekoDaemon"
    private const val LIB_NAME = "nekodaemon"
    private var nativeLoaded = false
    private var mBinder: IBinder? = null

    private external fun startDaemon(apkPath: String, nativeLibDir: String)
    private external fun stopDaemon()
    private external fun nativeGetUptime(): Int

    fun loadNative(): Boolean {
        if (nativeLoaded) return true
        return try {
            System.loadLibrary(LIB_NAME)
            nativeLoaded = true
            Log.i(TAG, "Native library loaded")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library not available: ${e.message}")
            false
        }
    }

    fun start(context: Context): Boolean {
        if (!loadNative()) return false
        if (!Shell.checkRootAccess()) {
            Log.w(TAG, "Root not available")
            return false
        }
        return try {
            startDaemon(context.applicationInfo.sourceDir, context.applicationInfo.nativeLibraryDir)
            Log.i(TAG, "Daemon start requested")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start daemon", e)
            false
        }
    }

    fun stop() {
        try {
            stopDaemon()
        } catch (_: Exception) {}
        Shell.execute("kill -9 \$(pgrep -f nekodaemon) 2>/dev/null")
        Log.i(TAG, "Daemon stopped")
    }

    val uptime: Int get() = try { nativeGetUptime() } catch (_: Exception) { -1 }

    var binder: IBinder?
        get() = mBinder
        set(value) { mBinder = value }

    val connected: Boolean get() = mBinder?.pingBinder() == true
}
