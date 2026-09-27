package com.hourlyvoiceclock.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferredEngineLabelTest {

    @Test
    fun `google engine uses the short product name`() {
        // Regression: ISSUE-003 — platform label was clipped to "Speech Recognition"
        // Found by /qa on 2026-09-27
        // Report: .gstack/qa-reports/qa-report-hourly-voice-clock-2026-09-27.md
        assertEquals(
            "Speech Services by Google",
            preferredEngineLabel(
                "com.google.android.tts",
                "Speech Recognition and Synthesis from Google"
            )
        )
    }

    @Test
    fun `unknown engines keep the platform label`() {
        assertEquals(
            "Acme TTS",
            preferredEngineLabel("com.example.tts", "Acme TTS")
        )
    }
}
