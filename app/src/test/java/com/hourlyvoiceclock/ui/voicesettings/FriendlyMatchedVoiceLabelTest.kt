package com.hourlyvoiceclock.ui.voicesettings

import com.hourlyvoiceclock.tts.VoiceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FriendlyMatchedVoiceLabelTest {

    @Test
    fun `selected special voice shows the friendly description not the engine id`() {
        // Regression: ISSUE-004 — selected preset appended "en-us-x-sfg-local"
        // Found by /qa on 2026-09-27
        // Report: .gstack/qa-reports/qa-report-hourly-voice-clock-2026-09-27.md
        val voicesByName = mapOf(
            "en-us-x-sfg-local" to voice(name = "en-us-x-sfg-local", description = "United States Voice 2")
        )

        assertEquals(
            "United States Voice 2",
            friendlyMatchedVoiceLabel(voicesByName, "en-us-x-sfg-local")
        )
    }

    @Test
    fun `missing voice does not fall back to the raw engine id`() {
        assertNull(friendlyMatchedVoiceLabel(emptyMap(), "en-us-x-sfg-local"))
        assertNull(friendlyMatchedVoiceLabel(emptyMap(), null))
    }

    private fun voice(name: String, description: String) = VoiceInfo(
        name = name,
        localeDisplayName = "United States",
        localeTag = "en-US",
        quality = 400,
        latency = 200,
        requiresNetwork = false,
        genderLabel = "Female",
        description = description,
        isSpecial = false
    )
}
