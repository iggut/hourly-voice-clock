package com.hourlyvoiceclock.tts

import com.hourlyvoiceclock.data.AudioChannel

/**
 * TTS configuration profile passed to [TtsEngine.configure].
 *
 * Bundles the user's voice selection, locale, pitch, rate, and audio
 * channel into one value object. Each engine adapter picks the fields
 * it understands: the system engine uses [voiceName] and [localeTag],
 * while the local engine prefers [localModelId].
 */
data class VoiceProfile(
    val voiceName: String?,
    val localeTag: String?,
    val localModelId: String?,
    val pitch: Float,
    val speechRate: Float,
    val audioChannel: AudioChannel
)
