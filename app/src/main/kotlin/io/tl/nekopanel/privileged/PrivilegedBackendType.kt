package io.tl.nekopanel.privileged

enum class PrivilegedBackendType(val value: String) {
    Shizuku("shizuku"),
    Root("root");

    companion object {
        fun from(value: String): PrivilegedBackendType = entries.firstOrNull { it.value == value } ?: Shizuku
    }
}
