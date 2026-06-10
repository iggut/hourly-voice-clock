package com.hourlyvoiceclock.announcer

import org.junit.Assert.assertEquals
import org.junit.Test

class HapticPulseTest {

    @Test
    fun `default pulse duration is 200ms`() {
        assertEquals(200L, HapticPulse.DEFAULT_PULSE_MS)
    }
}
