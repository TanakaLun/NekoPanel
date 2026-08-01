package io.tl.nekopanel.model

import org.json.JSONObject

data class ConfigInfo(
    val mode: String,
    val modes: List<String>,
    val json: JSONObject,
) {
    fun has(key: String): Boolean = json.has(key)
}
