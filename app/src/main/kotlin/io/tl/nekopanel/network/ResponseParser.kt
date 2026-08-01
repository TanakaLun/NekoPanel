package io.tl.nekopanel.network

import io.tl.nekopanel.model.ConfigInfo
import io.tl.nekopanel.model.RuleInfo
import io.tl.nekopanel.model.TrafficSample
import org.json.JSONObject

object ResponseParser {

    fun parseTraffic(text: String): TrafficSample {
        val obj = JSONObject(text)
        val up = obj.optLong("up", 0L)
        val down = obj.optLong("down", 0L)
        val hasTotals = obj.has("downTotal") && obj.has("upTotal")
        val upTotal = obj.optLong("upTotal", 0L)
        val downTotal = obj.optLong("downTotal", 0L)
        val upCumulative = if (obj.has("upCumulative")) obj.optLong("upCumulative", -1L) else -1L
        val downCumulative = if (obj.has("downCumulative")) obj.optLong("downCumulative", -1L) else -1L
        return TrafficSample(up, down, upTotal, downTotal, upCumulative, downCumulative, hasTotals)
    }

    fun parseRules(text: String, backend: BackendType): List<RuleInfo> {
        val arr = JSONObject(text).getJSONArray("rules")
        val list = mutableListOf<RuleInfo>()
        for (i in 0 until arr.length()) {
            val rule = arr.getJSONObject(i)
            val type = rule.optString("type", "")
            val payload = rule.optString("payload", "")
            val proxy = rule.optString("proxy", "")
            if (backend == BackendType.SING_BOX) {
                val uuid = rule.optString("uuid", "")
                val id = uuid.ifBlank { i.toString() }
                val disabled = rule.optBoolean("disabled", false)
                list.add(RuleInfo(id, type, payload, proxy, disabled, 0L, false))
            } else {
                val index = if (rule.has("index")) rule.getInt("index") else i
                val extra = rule.optJSONObject("extra")
                val disabled = extra?.optBoolean("disabled", false) ?: false
                val hitCount = extra?.optLong("hitCount", 0L) ?: 0L
                list.add(RuleInfo(index.toString(), type, payload, proxy, disabled, hitCount, extra != null))
            }
        }
        return list
    }

    fun parseConfigs(text: String): ConfigInfo {
        val obj = JSONObject(text)
        val mode = obj.optString("mode", "rule")
        val modes = mutableListOf<String>()
        val modeList = obj.optJSONArray("mode-list")
        if (modeList != null) {
            for (i in 0 until modeList.length()) modes.add(modeList.getString(i))
        }
        val alt = obj.optJSONArray("modes")
        if (alt != null) {
            for (i in 0 until alt.length()) {
                val m = alt.getString(i)
                if (m !in modes) modes.add(m)
            }
        }
        return ConfigInfo(mode, modes, obj)
    }
}
