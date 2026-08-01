package io.tl.nekopanel.model

data class RuleInfo(
    val id: String,
    val type: String,
    val payload: String,
    val proxy: String,
    val disabled: Boolean,
    val hitCount: Long,
    val hasHitStats: Boolean,
)
