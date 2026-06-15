package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.tts.TtsEngine
import java.time.LocalDateTime

class TimeAnnouncer(
    private val context: Context,
    private val ttsEngine: TtsEngine,
    private val chimePlayer: ChimePlayer,
    private val notifier: AnnouncementNotifier,
    private val hapticPulse: HapticPulse = HapticPulse(context),
    private val ttsConfig: TtsConfigApplier = TtsConfigApplier(ttsEngine),
    private val audioFocusController: AudioFocusController = AudioFocusController(context)
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
                settings.quietHoursEnd,
                settings.quietDaysDisabled,
                dateTime.dayOfWeek
            )
            if (inQuiet) {
                Log.d(TAG, "Blocked by quiet hours")
                return
            }
        }

        val channelSpec = AudioChannelMapping.specOf(settings.audioChannel)
        val audioStream = channelSpec.stream
        val usage = channelSpec.usage

        // Check volume on the selected stream
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val streamVolume = audioManager?.getStreamVolume(audioStream) ?: 0
        val streamMax = audioManager?.getStreamMaxVolume(audioStream) ?: 1
        if (streamVolume == 0) {
            Toast.makeText(context, "${channelSpec.shortLabel} volume is muted. Turn up volume to hear announcements.", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Stream volume is 0 for $audioStream - cannot hear TTS")
            return
        }
        Log.d(TAG, "Stream $audioStream volume: $streamVolume / $streamMax")

        if (settings.vibrateBefore) {
            hapticPulse.pulse()
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

        ttsConfig.apply(
            voiceName = settings.selectedVoiceName,
            savedLocale = settings.selectedLocale,
            pitch = settings.pitch,
            speechRate = settings.speechRate,
            audioChannel = settings.audioChannel
        )

        val text = AnnouncementFormatter.format(
            dateTime = dateTime,
            settings = settings,
            includeDate = includeDate && settings.announceDateOnDemand
        )

        Log.d(TAG, "Speaking: \"$text\" on channel=${settings.audioChannel}")

        // Auto-releases after AudioFocusController's release delay; the
        // returned handle is intentionally ignored — focus is abandoned
        // by the timer.
        audioFocusController.acquire(usage, audioStream)

        ttsEngine.speakAsync(text) { /* completion handled by engine */ }

        if (settings.notificationLogging) {
            notifier.post(text)
        }
    }

    companion object {
        private const val TAG = "TimeAnnouncer"
    }
}
