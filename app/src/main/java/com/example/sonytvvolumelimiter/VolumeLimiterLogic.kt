package com.example.sonytvvolumelimiter

object VolumeLimiterLogic {
    fun normalizeLimit(configuredLimit: Int, systemMax: Int): Int =
        configuredLimit.coerceIn(0, systemMax.coerceAtLeast(0))

    fun clampVolume(currentVolume: Int, configuredLimit: Int, systemMax: Int): Int {
        val safeSystemMax = systemMax.coerceAtLeast(0)
        val safeLimit = normalizeLimit(configuredLimit, safeSystemMax)
        return currentVolume.coerceIn(0, safeSystemMax).coerceAtMost(safeLimit)
    }

    fun shouldConsumeVolumeUp(currentVolume: Int, configuredLimit: Int, systemMax: Int): Boolean =
        currentVolume >= normalizeLimit(configuredLimit, systemMax)
}
