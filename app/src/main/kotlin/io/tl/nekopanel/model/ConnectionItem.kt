package io.tl.nekopanel.model

data class ConnectionItem(
    val id: String,
    val host: String,
    val network: String,
    val proxy: String,
    val upload: Long,
    val download: Long,
    val rawJson: String
)
