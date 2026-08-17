package com.example.sonytvvolumelimiter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun volumeUpBelowLimitIsPassedToAndroid() {
        assertFalse(VolumeLimiterLogic.shouldConsumeVolumeUp(6, 7, 15))
    }

    @Test
    fun volumeUpAtLimitIsConsumed() {
        assertTrue(VolumeLimiterLogic.shouldConsumeVolumeUp(7, 7, 15))
        assertTrue(VolumeLimiterLogic.shouldConsumeVolumeUp(12, 7, 15))
    }

    @Test
    fun zeroLimitConsumesAnyVolumeUp() {
        assertTrue(VolumeLimiterLogic.shouldConsumeVolumeUp(0, 0, 15))
    }
}
