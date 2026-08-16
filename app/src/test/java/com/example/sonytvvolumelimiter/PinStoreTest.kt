package com.example.sonytvvolumelimiter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinStoreTest {
    @Test
    fun acceptsExactlyFourDigits() {
        assertTrue(PinStore.isValidPin("0427"))
    }

    @Test
    fun rejectsWrongLengthAndNonDigits() {
        assertFalse(PinStore.isValidPin("123"))
        assertFalse(PinStore.isValidPin("12345"))
        assertFalse(PinStore.isValidPin("12a4"))
    }
}
