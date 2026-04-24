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

    // 累计流量（持久化，用于跨核心生命周期统计）
    private fun saveCumulativeTraffic(down: Long, up: Long) {
        prefs.edit()
            .putLong("cumulative_down", down)
            .putLong("cumulative_up", up)
            .apply()
    }

    /** 获取上次保存的累计流量值 */
    fun getCumulativeTraffic(): Pair<Long, Long> {
        return prefs.getLong("cumulative_down", 0L) to prefs.getLong("cumulative_up", 0L)
    }

    /**
     * 更新累计流量。自动处理核心重启导致的数据回退：
     * 如果新的原始总流量小于上次记录的原始值，说明核心已经重启，此时将差值累加到累计值。
     * rawDown/rawUp：直接从 /traffic 获取的 downTotal/upTotal
     * lastRawDown/lastRawUp：上次记录的原始总流量（用于判断回退）
     */
    fun updateCumulativeTraffic(
        rawDown: Long, rawUp: Long,
        lastRawDown: Long, lastRawUp: Long
    ) {
        val (cumDown, cumUp) = getCumulativeTraffic()
        val newCumDown = if (rawDown >= lastRawDown) {
            cumDown + (rawDown - lastRawDown)
        } else {
            cumDown + rawDown // 重启后直接累加当前原始值
        }
        val newCumUp = if (rawUp >= lastRawUp) {
            cumUp + (rawUp - lastRawUp)
        } else {
            cumUp + rawUp
        }
        saveCumulativeTraffic(newCumDown, newCumUp)
    }

    /** 保存最后一次原始流量值（用于下次比较） */
    fun saveLastRawTraffic(down: Long, up: Long) {
        prefs.edit()
            .putLong("last_raw_down", down)
            .putLong("last_raw_up", up)
            .apply()
    }

    fun getLastRawTraffic(): Pair<Long, Long> {
        return prefs.getLong("last_raw_down", 0L) to prefs.getLong("last_raw_up", 0L)
    }
}