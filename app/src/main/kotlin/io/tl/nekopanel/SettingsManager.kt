package io.tl.nekopanel

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("mihomo_settings", Context.MODE_PRIVATE)

    // 现有字段保持不变 ...
    var testUrl: String
        get() = prefs.getString("test_url", "http://www.gstatic.com/generate_204") ?: "http://www.gstatic.com/generate_204"
        set(value) = prefs.edit().putString("test_url", value).apply()

    var testTimeout: Int
        get() = prefs.getInt("test_timeout", 5000)
        set(value) = prefs.edit().putInt("test_timeout", value).apply()

    var logLevel: String
        get() = prefs.getString("log_level", "info") ?: "info"
        set(value) = prefs.edit().putString("log_level", value).apply()

    var backgroundWebSocket: Boolean
        get() = prefs.getBoolean("background_websocket", false)
        set(value) = prefs.edit().putBoolean("background_websocket", value).apply()

    var pureBlackMode: Boolean
        get() = prefs.getBoolean("pure_black_mode", false)
        set(value) = prefs.edit().putBoolean("pure_black_mode", value).apply()

    var groupColumnCount: Int
        get() = prefs.getInt("group_column_count", 1)
        set(value) = prefs.edit().putInt("group_column_count", value).apply()

    var columnCount: Int
        get() = prefs.getInt("column_count", 2)
        set(value) = prefs.edit().putInt("column_count", value).apply()

    var groupBadgeStyle: String
        get() = prefs.getString("group_badge_style", "填充") ?: "填充"
        set(value) = prefs.edit().putString("group_badge_style", value).apply()

    var delayBadgeStyle: String
        get() = prefs.getString("delay_badge_style", "描边") ?: "描边"
        set(value) = prefs.edit().putString("delay_badge_style", value).apply()

    var ruleBadgeStyle: String
        get() = prefs.getString("rule_badge_style", "描边") ?: "描边"
        set(value) = prefs.edit().putString("rule_badge_style", value).apply()

    var badgeCornerRadius: Int
        get() = prefs.getInt("badge_corner_radius", 8)
        set(value) = prefs.edit().putInt("badge_corner_radius", value).apply()

    var showGlobal: Boolean
        get() = prefs.getBoolean("show_global", true)
        set(value) = prefs.edit().putBoolean("show_global", value).apply()

    var useSheetMode: Boolean
        get() = prefs.getBoolean("use_sheet_mode", false)
        set(value) = prefs.edit().putBoolean("use_sheet_mode", value).apply()

    var cardFillStyle: Boolean
        get() = prefs.getBoolean("card_fill_style", false)
        set(value) = prefs.edit().putBoolean("card_fill_style", value).apply()

    // ------------- 新增持久化字段 -------------

    /** 核心 API 地址（包含 http://IP:PORT） */
    var apiBaseUrl: String
        get() = prefs.getString("api_base_url", "") ?: ""
        set(value) = prefs.edit().putString("api_base_url", value).apply()

    /** API 密钥（Bearer Token），可为空 */
    var apiSecret: String
        get() = prefs.getString("api_secret", "") ?: ""
        set(value) = prefs.edit().putString("api_secret", value).apply()

    fun getCumulativeTraffic(): Pair<Long, Long> {
        return prefs.getLong("cumulative_down", 0L) to prefs.getLong("cumulative_up", 0L)
    }

    fun getLastTraffic(): Pair<Long, Long> {
        return prefs.getLong("last_total_down", 0L) to prefs.getLong("last_total_up", 0L)
    }

    fun saveLastTraffic(down: Long, up: Long) {
        prefs.edit().putLong("last_total_down", down).putLong("last_total_up", up).apply()
    }

    fun accumulateTraffic(totalDown: Long, totalUp: Long, maxDelta: Long = 10_000_000_000_000L) {
        val (lastDown, lastUp) = getLastTraffic()
        val deltaDown = totalDown - lastDown
        val deltaUp = totalUp - lastUp
        val validDown = deltaDown in 0..maxDelta
        val validUp = deltaUp in 0..maxDelta

        val (cumDown, cumUp) = getCumulativeTraffic()
        val newCumDown = if (validDown) cumDown + deltaDown else cumDown
        val newCumUp = if (validUp) cumUp + deltaUp else cumUp

        prefs.edit()
            .putLong("cumulative_down", newCumDown)
            .putLong("cumulative_up", newCumUp)
            .apply()

        saveLastTraffic(totalDown, totalUp)
    }

    /** 重置累计流量计数器 */
    fun resetCumulativeTraffic() {
        prefs.edit()
            .putLong("cumulative_down", 0L)
            .putLong("cumulative_up", 0L)
            .putLong("last_total_down", 0L)
            .putLong("last_total_up", 0L)
            .apply()
    }
}