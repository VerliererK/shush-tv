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

}
