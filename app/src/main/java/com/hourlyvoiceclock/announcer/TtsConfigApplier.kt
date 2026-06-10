package com.hourlyvoiceclock.announcer

import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.tts.TtsEngine
import java.util.Locale

/**
 * Applies a user's [com.hourlyvoiceclock.data.AppSettings] to a [TtsEngine].
 *
 * Encapsulates the voice/language fallback cascade that previously lived
 * inline in TimeAnnouncer.speakText(): try the saved voice, then the saved
 * locale, then the device default, then hard-fallback to `en-US`. The
 * remaining TTS parameters (pitch, speech rate, audio channel) are applied
 * unconditionally once a voice has been chosen.
 *
 * Extracted so the cascade can be unit-tested with a fake engine, and so
 * the announcer stops being responsible for "how do I pick a voice to
 * speak with."
 */
class TtsConfigApplier(private val engine: TtsEngine) {

    /**
     * Apply the given TTS configuration to [engine]. Returns `true` if a
     * specific voice (either the saved one or one of the locale fallbacks)
     * was successfully selected. The `deviceDefaultLocale` parameter
     * defaults to the runtime's current default but is overridable so
     * tests can pin a deterministic device locale.
     */
    fun apply(
        voiceName: String?,
        savedLocale: String?,
        pitch: Float,
        speechRate: Float,
        audioChannel: AudioChannel,
        deviceDefaultLocale: String = Locale.getDefault().toLanguageTag()
    ): Boolean {
        val voiceSet = selectVoiceOrLocale(voiceName, savedLocale, deviceDefaultLocale)

        engine.setPitch(pitch)
        engine.setSpeechRate(speechRate)
        engine.setAudioChannel(audioChannel)

        return voiceSet
    }

    /**
     * Walk the voice-selection cascade. Public-internal so the test can
     * exercise the cascade without touching pitch/rate/channel plumbing.
     */
    internal fun selectVoiceOrLocale(
        voiceName: String?,
        savedLocale: String?,
        deviceDefaultLocale: String
    ): Boolean {
        if (!voiceName.isNullOrBlank()) {
            val localeTag = savedLocale?.takeIf { it.isNotBlank() } ?: ""
            if (engine.setVoice(voiceName, localeTag)) return true
        }
        if (!savedLocale.isNullOrBlank() && engine.setLanguage(savedLocale)) return true
        if (deviceDefaultLocale.isNotBlank() && engine.setLanguage(deviceDefaultLocale)) return true
        // Last resort. setLanguage is allowed to return false here — at
        // least we tried to give the engine *something* to speak.
        engine.setLanguage(FALLBACK_LOCALE)
        return false
    }

    companion object {
        const val FALLBACK_LOCALE = "en-US"
    }
}
