package com.hourlyvoiceclock.announcer

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFocusControllerTest {

    @Test
    fun `default release delay is 5 seconds`() {
        assertEquals(5000L, AudioFocusController.DEFAULT_RELEASE_DELAY_MS)
    }
}
