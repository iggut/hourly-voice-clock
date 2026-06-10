package com.hourlyvoiceclock.announcer

import android.media.AudioAttributes
import android.media.AudioManager
import com.hourlyvoiceclock.data.AudioChannel

/**
 * Translates an [AudioChannel] (a settings-level concept) to the
 * Android-framework audio constants the platform needs in order to
 * route and duck audio.
 *
 * Centralising this mapping fixes three things the previous code had
 * wrong:
 *  1. the [AudioChannel] mapping to [AudioManager.STREAM_*] was
 *     duplicated in [com.hourlyvoiceclock.announcer.TimeAnnouncer] and
 *     [com.hourlyvoiceclock.tts.TtsEngine];
 *  2. the [AudioChannel] mapping to [AudioAttributes.USAGE_*] was
 *     duplicated in [TimeAnnouncer] and [TtsEngine.setupAudioAttributes];
 *  3. the [AudioChannel.CALL] case was internally inconsistent:
 *     TimeAnnouncer said `STREAM_VOICE_CALL` + `USAGE_VOICE_COMMUNICATION`
 *     while TtsEngine said `STREAM_RING` + `USAGE_NOTIFICATION_RINGTONE`.
 *
 * One [AudioChannelSpec] per channel, one place to add a new channel,
 * one place to keep the answer for "what does CALL actually mean to
 * this app" in sync.
 */
data class AudioChannelSpec(
    /** The [AudioManager] stream to use for volume + focus. */
    val stream: Int,
    /** The [AudioAttributes] usage hint for routing and ducking. */
    val usage: Int,
    /** The [AudioAttributes] content type — speech for announcements. */
    val contentType: Int,
    /** Short, human-readable label for toasts and logs. */
    val shortLabel: String
)

object AudioChannelMapping {

    fun specOf(channel: AudioChannel): AudioChannelSpec = when (channel) {
        AudioChannel.MEDIA -> AudioChannelSpec(
            stream = AudioManager.STREAM_MUSIC,
            usage = AudioAttributes.USAGE_MEDIA,
            contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
            shortLabel = "Media"
        )
        AudioChannel.NOTIFICATION -> AudioChannelSpec(
            stream = AudioManager.STREAM_NOTIFICATION,
            usage = AudioAttributes.USAGE_NOTIFICATION,
            contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
            shortLabel = "Notification"
        )
        AudioChannel.CALL -> AudioChannelSpec(
            // Use voice-comm attributes so the announcement routes
            // through the same hardware as a phone call. The legacy
            // code here was inconsistent — TtsEngine used
            // USAGE_NOTIFICATION_RINGTONE / STREAM_RING, which is
            // what a ringtone does, not what a call does.
            stream = AudioManager.STREAM_VOICE_CALL,
            usage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
            contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
            shortLabel = "Call"
        )
    }
}
