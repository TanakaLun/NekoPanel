package io.tl.nekopanel.util

import kotlin.math.log10
import kotlin.math.pow

fun Long.formatSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)
    return String.format(
        java.util.Locale.getDefault(),
        "%.2f %s",
        this / 1024.0.pow(digitGroups),
        units[digitGroups]
    )
}
