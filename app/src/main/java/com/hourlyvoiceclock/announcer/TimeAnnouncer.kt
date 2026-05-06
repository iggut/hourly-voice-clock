package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import java.time.LocalDateTime
import java.util.Locale

class TimeAnnouncer(
    private val context: Context,
    private val ttsRepository: TtsVoiceRepository
) {

    fun announce(settings: AppSettings, force: Boolean = false, includeDate: Boolean = false) {
        val now = LocalDateTime.now()

        if (!force) {
            val inQuiet = QuietHoursPolicy.isQuietTime(
                now.toLocalTime(),
                settings.quietHoursEnabled,
                settings.quietHoursStart,
                settings.quietHoursEnd
            )
            if (inQuiet) {
                Log.d("TimeAnnouncer", "Blocked by quiet hours")
                return
            }
        }

        // Check media volume
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val musicVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val musicMax = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        if (musicVolume == 0) {
            Toast.makeText(context, "Media volume is muted. Turn up volume to hear announcements.", Toast.LENGTH_LONG).show()
            Log.w("TimeAnnouncer", "Media volume is 0 - cannot hear TTS")
            return
        }
        Log.d("TimeAnnouncer", "Media volume: $musicVolume / $musicMax")

        if (settings.vibrateBefore) {
            vibrate()
        }

        if (!ttsRepository.isAvailable()) {
            Log.w("TimeAnnouncer", "TTS not available - attempting init")
        }

        val voiceSet = settings.selectedVoiceName?.let { voiceName ->
            ttsRepository.selectVoice(voiceName, settings.selectedLocale ?: "")
        } ?: false

        if (!voiceSet) {
            val localeSet = settings.selectedLocale?.let { locale ->
                ttsRepository.selectLanguage(locale)
            } ?: false
            if (!localeSet) {
                val deviceDefault = Locale.getDefault().toLanguageTag()
                val defaultSet = ttsRepository.selectLanguage(deviceDefault)
                if (!defaultSet) {
                    // Try English as last resort
                    ttsRepository.selectLanguage("en-US")
                }
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

        Log.d("TimeAnnouncer", "Speaking: \"$text\"")

        var focusRequest: AudioFocusRequest? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
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

        ttsRepository.speak(text)

        // Release audio focus after a short delay (fire-and-forget)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        }, 5000)
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
