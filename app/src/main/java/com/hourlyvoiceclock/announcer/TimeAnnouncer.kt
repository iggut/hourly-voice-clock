package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import java.time.LocalDateTime
import java.util.Locale

class TimeAnnouncer(
    private val context: Context,
    private val ttsRepository: TtsVoiceRepository
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
                Log.d("TimeAnnouncer", "Blocked by quiet hours")
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
            Log.w("TimeAnnouncer", "Stream volume is 0 for $audioStream - cannot hear TTS")
            return
        }
        Log.d("TimeAnnouncer", "Stream $audioStream volume: $streamVolume / $streamMax")

        if (settings.vibrateBefore) {
            vibrate()
        }

        if (settings.chimeSound != ChimeSound.NONE) {
            playChime(settings.chimeSound)
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
                    ttsRepository.selectLanguage("en-US")
                }
            }
        }

        ttsRepository.setPitch(settings.pitch)
        ttsRepository.setSpeechRate(settings.speechRate)
        ttsRepository.setAudioChannel(settings.audioChannel)

        val text = AnnouncementFormatter.format(
            dateTime = dateTime,
            settings = settings,
            includeDate = includeDate && settings.announceDateOnDemand
        )

        Log.d("TimeAnnouncer", "Speaking: \"$text\" on channel=${settings.audioChannel}")

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

        ttsRepository.speak(text)

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
                Log.w("TimeAnnouncer", "Notification logging enabled but POST_NOTIFICATIONS is denied")
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

    private fun playChime(chimeSound: ChimeSound) {
        if (chimeSound == ChimeSound.NONE) return

        val resourceId = getChimeResourceId(chimeSound)
        if (resourceId == 0) {
            Log.w("TimeAnnouncer", "No resource found for chime sound: $chimeSound")
            return
        }

        try {
            val mediaPlayer = MediaPlayer.create(context, resourceId)
            if (mediaPlayer != null) {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
            } else {
                Log.w("TimeAnnouncer", "Could not create MediaPlayer for chime: $chimeSound")
            }
        } catch (e: Exception) {
            Log.e("TimeAnnouncer", "Error playing chime: $chimeSound", e)
        }
    }

    private fun getChimeResourceId(chimeSound: ChimeSound): Int {
        return when (chimeSound) {
            ChimeSound.NONE -> 0
            ChimeSound.CLASSIC_CHIME -> R.raw.classic_chime
            ChimeSound.BELL -> R.raw.bell
            ChimeSound.GONG -> R.raw.gong
            ChimeSound.CYMBALS -> R.raw.cymbals
            ChimeSound.DIGITAL_BEEP -> R.raw.digital_beep
            ChimeSound.BIRD_CHIRP -> R.raw.bird_chirp
            ChimeSound.HONK -> R.raw.honk
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
