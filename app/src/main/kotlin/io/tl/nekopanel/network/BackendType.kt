package io.tl.nekopanel.network

enum class BackendType(val displayName: String) {
    MIHOMO("mihomo"),
    SING_BOX("sing-box"),
    UNKNOWN("unknown");

    val isSingBox: Boolean get() = this == SING_BOX
    val isMihomo: Boolean get() = this == MIHOMO || this == UNKNOWN
}
