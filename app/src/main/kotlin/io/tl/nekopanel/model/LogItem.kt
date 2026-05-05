package io.tl.nekopanel.model

data class LogItem(
    val type: String,
    val payload: String,
    val time: Long = System.currentTimeMillis()
)
