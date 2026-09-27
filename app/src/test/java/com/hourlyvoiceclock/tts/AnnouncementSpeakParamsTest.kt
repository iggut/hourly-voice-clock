package com.hourlyvoiceclock.tts

import android.media.AudioManager
import android.speech.tts.TextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnnouncementSpeakParamsTest {

    @Test
    fun `stream type is an int so the engine does not drop it`() {
        // Regression: ISSUE-002 — KEY_PARAM_STREAM was a String and TTS ignored it
        // Found by /qa on 2026-09-27
        // Report: .gstack/qa-reports/qa-report-hourly-voice-clock-2026-09-27.md
        val params = announcementSpeakParams("hvc_1", AudioManager.STREAM_NOTIFICATION)

        assertEquals(
            AudioManager.STREAM_NOTIFICATION,
            params.getInt(TextToSpeech.Engine.KEY_PARAM_STREAM, -1)
        )
        assertEquals("hvc_1", params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }
}
