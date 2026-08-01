package io.tl.nekopanel.data.repository

/**
 * In-memory up/down totals for the current app session (reset when the process is
 * killed). sing-box's `/traffic` only reports per-second speeds, so the session
 * totals are accumulated here, mirroring mihomo's process-start `downTotal/upTotal`.
 * Shared across the Activity and the background traffic service (same process) via
 * the `backgroundServiceRunning` guard in the callers.
 */
object SessionTraffic {
    @Volatile
    var downTotal: Long = 0L
        private set

    @Volatile
    var upTotal: Long = 0L
        private set

    fun accumulate(down: Long, up: Long) {
        downTotal += down
        upTotal += up
    }

    fun reset() {
        downTotal = 0L
        upTotal = 0L
    }
}
