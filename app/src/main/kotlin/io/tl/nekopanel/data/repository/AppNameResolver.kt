package io.tl.nekopanel.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves Android package names (as reported by the core's connection metadata)
 * to user-friendly app labels by querying the installed applications list.
 *
 * Requires android.permission.QUERY_ALL_PACKAGES to see all packages on API 30+.
 * The map is built once on [ensureLoaded] and cached; on failure it degrades to
 * returning null so callers fall back to showing the raw process/package name.
 */
object AppNameResolver {

    private val cache = ConcurrentHashMap<String, String>()
    private val loadMutex = Mutex()

    @Volatile
    private var initialized = false

    suspend fun ensureLoaded(context: Context) {
        if (initialized) return
        loadMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) { buildMap(context.applicationContext) }
        }
    }

    fun resolve(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return cache[packageName]
    }

    fun reset() {
        cache.clear()
        initialized = false
    }

    private fun buildMap(context: Context) {
        val pm = context.packageManager
        val apps = runCatching { installedApps(pm) }.getOrElse { return }
        val map = HashMap<String, String>(apps.size)
        for (app in apps) {
            val label = runCatching { pm.getApplicationLabel(app).toString() }.getOrNull()
            if (label.isNullOrBlank()) continue
            map[app.packageName] = label
        }
        cache.clear()
        cache.putAll(map)
        initialized = true
    }

    @Suppress("DEPRECATION")
    private fun installedApps(pm: PackageManager): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getInstalledApplications(0)
        }
    }
}
