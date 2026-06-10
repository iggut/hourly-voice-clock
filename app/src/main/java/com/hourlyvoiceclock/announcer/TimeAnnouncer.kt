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
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.tts.TtsEngine
import java.time.LocalDateTime
import java.util.Locale

class TimeAnnouncer(
    private val context: Context,
    private val ttsEngine: TtsEngine,
    private val chimePlayer: ChimePlayer = ChimePlayer(context)
) {

    fun announce(
        settings: AppSettings,
        force: Boolean = false,
        includeDate: Boolean = false,
        dateTime: LocalDateTime = LocalDateTime.now()
    ) {

        if (!force) {
            val inQuiet = QuietHoursPolicy.isQuietTime(
                dateTime.toLocalTime(),
                settings.quietHoursEnabled,
                settings.quietHoursStart,
                settings.quietHoursEnd
            )
            if (inQuiet) {
                Log.d(TAG, "Blocked by quiet hours")
                return
            }
        }

        val (audioStream, usage) = when (settings.audioChannel) {
            AudioChannel.MEDIA -> AudioManager.STREAM_MUSIC to AudioAttributes.USAGE_MEDIA
            AudioChannel.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION to AudioAttributes.USAGE_NOTIFICATION
            AudioChannel.CALL -> AudioManager.STREAM_VOICE_CALL to AudioAttributes.USAGE_VOICE_COMMUNICATION
        }

        // Check volume on the selected stream
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val streamVolume = audioManager?.getStreamVolume(audioStream) ?: 0
        val streamMax = audioManager?.getStreamMaxVolume(audioStream) ?: 1
        if (streamVolume == 0) {
            val label = when (settings.audioChannel) {
                AudioChannel.MEDIA -> "Media"
                AudioChannel.NOTIFICATION -> "Notification"
                AudioChannel.CALL -> "Call"
            }
            Toast.makeText(context, "$label volume is muted. Turn up volume to hear announcements.", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Stream volume is 0 for $audioStream - cannot hear TTS")
            return
        }
        Log.d(TAG, "Stream $audioStream volume: $streamVolume / $streamMax")

        if (settings.vibrateBefore) {
            vibrate()
        }

        if (settings.chimeSound != ChimeSound.NONE) {
            chimePlayer.play(settings.chimeSound) {
                speakText(settings, dateTime, includeDate, audioManager, usage, audioStream)
            }
        } else {
            speakText(settings, dateTime, includeDate, audioManager, usage, audioStream)
        }
    }

    private fun speakText(
        settings: AppSettings,
        dateTime: LocalDateTime,
        includeDate: Boolean,
        audioManager: AudioManager?,
        usage: Int,
        audioStream: Int
    ) {
        if (!ttsEngine.isAvailable()) {
            Log.w(TAG, "TTS not available - attempting init")
        }

        val voiceSet = settings.selectedVoiceName?.let { voiceName ->
            ttsEngine.setVoice(voiceName, settings.selectedLocale ?: "")
        } ?: false

        if (!voiceSet) {
            val localeSet = settings.selectedLocale?.let { locale ->
                ttsEngine.setLanguage(locale)
            } ?: false
            if (!localeSet) {
                val deviceDefault = Locale.getDefault().toLanguageTag()
                val defaultSet = ttsEngine.setLanguage(deviceDefault)
                if (!defaultSet) {
                    ttsEngine.setLanguage("en-US")
                }
            }
        }

        ttsEngine.setPitch(settings.pitch)
        ttsEngine.setSpeechRate(settings.speechRate)
        ttsEngine.setAudioChannel(settings.audioChannel)

        val text = AnnouncementFormatter.format(
            dateTime = dateTime,
            settings = settings,
            includeDate = includeDate && settings.announceDateOnDemand
        )

        Log.d(TAG, "Speaking: \"$text\" on channel=${settings.audioChannel}")

        var focusRequest: AudioFocusRequest? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                audioStream,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        ttsEngine.speakAsync(text) { /* completion handled by engine */ }

        if (settings.notificationLogging) {
            postNotification(text)
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        }, 5000)
    }

    private fun postNotification(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.w(TAG, "Notification logging enabled but POST_NOTIFICATIONS is denied")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, com.hourlyvoiceclock.HourlyVoiceClockApp.CHANNEL_ID_STATUS)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
        val notification = builder
            .setContentTitle("Time Announced")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
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

    companion object {
        private const val TAG = "TimeAnnouncer"
        private const val NOTIFICATION_ID = 2001
    }
}
