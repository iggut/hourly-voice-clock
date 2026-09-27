package com.hourlyvoiceclock.announcer

import android.media.AudioAttributes
import com.hourlyvoiceclock.data.AudioChannel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChimeAudioAttributesTest {

    @Test
    fun `chime follows the announcement audio channel`() {
        // Regression: ISSUE-001 — chime ignored the user's audio channel
        // Found by /qa on 2026-09-27
        // Report: .gstack/qa-reports/qa-report-hourly-voice-clock-2026-09-27.md
        assertEquals(
            AudioAttributes.USAGE_MEDIA,
            chimeAudioAttributes(AudioChannel.MEDIA).usage
        )
        assertEquals(
            AudioAttributes.USAGE_NOTIFICATION,
            chimeAudioAttributes(AudioChannel.NOTIFICATION).usage
        )
        assertEquals(
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            chimeAudioAttributes(AudioChannel.CALL).usage
        )
        assertEquals(
            AudioAttributes.CONTENT_TYPE_SONIFICATION,
            chimeAudioAttributes(AudioChannel.MEDIA).contentType
        )
    }
}
