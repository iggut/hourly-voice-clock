package com.hourlyvoiceclock.announcer

import android.media.AudioAttributes
import android.media.AudioManager
import com.hourlyvoiceclock.data.AudioChannel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the single source of truth for "what does each AudioChannel
 * map to in Android audio constants". The previous code had this
 * mapping duplicated in 4 places with 3 different answers for the
 * CALL case.
 */
class AudioChannelMappingTest {

    @Test
    fun `MEDIA maps to music stream and media usage`() {
        val spec = AudioChannelMapping.specOf(AudioChannel.MEDIA)
        assertEquals(AudioManager.STREAM_MUSIC, spec.stream)
        assertEquals(AudioAttributes.USAGE_MEDIA, spec.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SPEECH, spec.contentType)
    }

    @Test
    fun `NOTIFICATION maps to notification stream and notification usage`() {
        val spec = AudioChannelMapping.specOf(AudioChannel.NOTIFICATION)
        assertEquals(AudioManager.STREAM_NOTIFICATION, spec.stream)
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, spec.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SPEECH, spec.contentType)
    }

    @Test
    fun `CALL maps to voice-call stream and voice-communication usage`() {
        // The previous code had CALL → STREAM_RING + USAGE_NOTIFICATION_RINGTONE
        // in TtsEngine, but STREAM_VOICE_CALL + USAGE_VOICE_COMMUNICATION in
        // TimeAnnouncer. The voice-comm answer is what the user picked
        // "Call" for; this test pins the resolution.
        val spec = AudioChannelMapping.specOf(AudioChannel.CALL)
        assertEquals(AudioManager.STREAM_VOICE_CALL, spec.stream)
        assertEquals(AudioAttributes.USAGE_VOICE_COMMUNICATION, spec.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SPEECH, spec.contentType)
    }

    @Test
    fun `every channel has a non-empty short label`() {
        AudioChannel.values().forEach { channel ->
            val label = AudioChannelMapping.specOf(channel).shortLabel
            assert(label.isNotBlank()) { "Missing short label for $channel" }
        }
    }

    @Test
    fun `every channel maps to a distinct short label`() {
        val labels = AudioChannel.values().map { AudioChannelMapping.specOf(it).shortLabel }
        assertEquals(labels.size, labels.toSet().size)
    }
}
