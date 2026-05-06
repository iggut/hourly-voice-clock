package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.Locale

class TimeAnnouncer(
    private val context: Context,
    private val ttsRepository: TtsVoiceRepository
) {

    suspend fun announce(settings: AppSettings, force: Boolean = false, includeDate: Boolean = false): Boolean {
        val now = LocalDateTime.now()

        if (!force) {
            val inQuiet = QuietHoursPolicy.isQuietTime(
                now.toLocalTime(),
                settings.quietHoursEnabled,
                settings.quietHoursStart,
                settings.quietHoursEnd
            )
            if (inQuiet) return false
        }

        if (settings.vibrateBefore) {
            vibrate()
        }

        val initOk = ttsRepository.initialize()
        if (!initOk) return false

        val voiceSet = settings.selectedVoiceName?.let { voiceName ->
            ttsRepository.selectVoice(voiceName, settings.selectedLocale ?: "")
        } ?: false

        if (!voiceSet) {
            val localeSet = settings.selectedLocale?.let { locale ->
                ttsRepository.selectLanguage(locale)
            } ?: false
            if (!localeSet) {
                ttsRepository.selectLanguage(Locale.getDefault().toLanguageTag())
            }
        }

        ttsRepository.setPitch(settings.pitch)
        ttsRepository.setSpeechRate(settings.speechRate)

        val text = AnnouncementFormatter.format(
            now,
            settings.timeFormat,
            settings.phraseStyle,
            includeDate && settings.announceDateOnDemand
        )

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        var focusRequest: AudioFocusRequest? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        return try {
            val spoke = ttsRepository.previewVoice(text)
            if (spoke) {
                // Some TTS engines report onDone when synthesis is handed to
                // AudioTrack, not when the last sample has reached the speaker.
                // Keep the engine alive briefly so shutdown() doesn't tear down
                // playback before audible audio is emitted.
                delay(2500)
            }
            spoke
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
