package com.hourlyvoiceclock.ui.voicesettings

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceControlFormatTest {

    @Test
    fun `tenths drop the trailing zero`() {
        assertEquals("1.0", formatVoiceControl(1f))
        assertEquals("0.9", formatVoiceControl(0.9f))
        assertEquals("2.0", formatVoiceControl(2f))
    }

    @Test
    fun `preset hundredths stay visible`() {
        assertEquals("0.55", formatVoiceControl(0.55f))
        assertEquals("0.78", formatVoiceControl(0.78f))
        assertEquals("0.88", formatVoiceControl(0.88f))
        assertEquals("1.65", formatVoiceControl(1.65f))
    }
}
