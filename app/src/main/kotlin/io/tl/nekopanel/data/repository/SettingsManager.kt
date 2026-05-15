package io.tl.nekopanel.data.repository

import android.content.Context
import io.tl.nekopanel.data.local.AppDatabase
import io.tl.nekopanel.data.local.SettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class SettingsManager(context: Context) {
    private val dao = AppDatabase.getInstance(context).settingsDao()
    private val cache = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        val preloaded = runBlocking {
            val entities = dao.getAll()
            entities.forEach { cache[it.key] = it.value }
            cache["data_migrated"] != null
        }
        if (!preloaded) {
            scope.launch { migrateFromSharedPreferences(context) }
        }
    }

    private suspend fun migrateFromSharedPreferences(context: Context) {
        val prefs = context.getSharedPreferences("mihomo_settings", Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            val strValue = when (value) {
                is String -> value
                is Int -> value.toString()
                is Long -> value.toString()
                is Boolean -> value.toString()
                is Float -> value.toString()
                else -> return@forEach
            }
            dao.put(SettingsEntity(key, strValue))
            cache[key] = strValue
        }
        dao.put(SettingsEntity("data_migrated", "true"))
        cache["data_migrated"] = "true"
    }

    private fun getString(key: String, default: String): String = cache[key] ?: default
    private fun setString(key: String, value: String) {
        cache[key] = value
        scope.launch { dao.put(SettingsEntity(key, value)) }
    }

    private fun getInt(key: String, default: Int): Int = cache[key]?.toIntOrNull() ?: default
    private fun setInt(key: String, value: Int) = setString(key, value.toString())
    private fun getBoolean(key: String, default: Boolean): Boolean = cache[key]?.toBooleanStrictOrNull() ?: default
    private fun setBoolean(key: String, value: Boolean) = setString(key, value.toString())
    private fun getLong(key: String, default: Long): Long = cache[key]?.toLongOrNull() ?: default
    private fun setLong(key: String, value: Long) = setString(key, value.toString())

    var testUrl: String
        get() = getString("test_url", "http://www.gstatic.com/generate_204")
        set(value) = setString("test_url", value)

    var testTimeout: Int
        get() = getInt("test_timeout", 5000)
        set(value) = setInt("test_timeout", value)

    var logLevel: String
        get() = getString("log_level", "info")
        set(value) = setString("log_level", value)

    var backgroundWebSocket: Boolean
        get() = getBoolean("background_websocket", false)
        set(value) = setBoolean("background_websocket", value)

    var continuousData: Boolean
        get() = getBoolean("continuous_data", false)
        set(value) = setBoolean("continuous_data", value)

    var pureBlackMode: Boolean
        get() = getBoolean("pure_black_mode", false)
        set(value) = setBoolean("pure_black_mode", value)

    var groupColumnCount: Int
        get() = getInt("group_column_count", 1)
        set(value) = setInt("group_column_count", value)

    var columnCount: Int
        get() = getInt("column_count", 2)
        set(value) = setInt("column_count", value)

    var groupBadgeStyle: String
        get() = getString("group_badge_style", "填充")
        set(value) = setString("group_badge_style", value)

    var delayBadgeStyle: String
        get() = getString("delay_badge_style", "描边")
        set(value) = setString("delay_badge_style", value)

    var ruleBadgeStyle: String
        get() = getString("rule_badge_style", "描边")
        set(value) = setString("rule_badge_style", value)

    var badgeCornerRadius: Int
        get() = getInt("badge_corner_radius", 8)
        set(value) = setInt("badge_corner_radius", value)

    var showGlobal: Boolean
        get() = getBoolean("show_global", true)
        set(value) = setBoolean("show_global", value)

    var useSheetMode: Boolean
        get() = getBoolean("use_sheet_mode", false)
        set(value) = setBoolean("use_sheet_mode", value)

    var cardFillStyle: Boolean
        get() = getBoolean("card_fill_style", false)
        set(value) = setBoolean("card_fill_style", value)

    var hideFromRecents: Boolean
        get() = getBoolean("hide_from_recents", false)
        set(value) = setBoolean("hide_from_recents", value)

    var apiBaseUrl: String
        get() = getString("api_base_url", "")
        set(value) = setString("api_base_url", value)

    var apiSecret: String
        get() = getString("api_secret", "")
        set(value) = setString("api_secret", value)

    fun getCumulativeTraffic(): Pair<Long, Long> {
        return getLong("cumulative_down", 0L) to getLong("cumulative_up", 0L)
    }

    fun getLastTraffic(): Pair<Long, Long> {
        return getLong("last_total_down", 0L) to getLong("last_total_up", 0L)
    }

    fun saveLastTraffic(down: Long, up: Long) {
        setLong("last_total_down", down)
        setLong("last_total_up", up)
    }

    @Synchronized
    fun accumulateTraffic(totalDown: Long, totalUp: Long, maxDelta: Long = 10_000_000_000_000L) {
        val (lastDown, lastUp) = getLastTraffic()
        val deltaDown = totalDown - lastDown
        val deltaUp = totalUp - lastUp
        val validDown = deltaDown in 0..maxDelta
        val validUp = deltaUp in 0..maxDelta

        val (cumDown, cumUp) = getCumulativeTraffic()
        val newCumDown = if (validDown) cumDown + deltaDown else cumDown
        val newCumUp = if (validUp) cumUp + deltaUp else cumUp

        setLong("cumulative_down", newCumDown)
        setLong("cumulative_up", newCumUp)
        setLong("last_total_down", totalDown)
        setLong("last_total_up", totalUp)
    }

    fun resetCumulativeTraffic() {
        setLong("cumulative_down", 0L)
        setLong("cumulative_up", 0L)
        setLong("last_total_down", 0L)
        setLong("last_total_up", 0L)
    }
}
