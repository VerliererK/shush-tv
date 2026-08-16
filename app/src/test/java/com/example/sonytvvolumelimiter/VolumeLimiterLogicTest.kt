package com.example.sonytvvolumelimiter

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeLimiterLogicTest {
    @Test
    fun volumeBelowLimitIsUnchanged() {
        assertEquals(4, VolumeLimiterLogic.clampVolume(4, 7, 15))
    }

    @Test
    fun volumeAboveLimitIsClamped() {
        assertEquals(7, VolumeLimiterLogic.clampVolume(12, 7, 15))
    }

    @Test
    fun configuredLimitCannotExceedSystemMaximum() {
        assertEquals(15, VolumeLimiterLogic.normalizeLimit(20, 15))
    }

    @Test
    fun volumeUpStopsAtConfiguredLimit() {
        assertEquals(7, VolumeLimiterLogic.nextVolumeUp(7, 7, 15))
        assertEquals(7, VolumeLimiterLogic.nextVolumeUp(6, 7, 15))
    }

    @Test
    fun repeatedVolumeUpNeverExceedsLimit() {
        var volume = 2
        repeat(20) {
            volume = VolumeLimiterLogic.nextVolumeUp(volume, 5, 15)
        }
        assertEquals(5, volume)
    }
}
