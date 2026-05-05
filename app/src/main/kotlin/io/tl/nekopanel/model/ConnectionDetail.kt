package io.tl.nekopanel.model

data class ConnectionDetail(
    val target: String,
    val process: String,
    val networkInfo: String,
    val routeNode: String,
    val rule: String,
    val startTimeMillis: Long
)
