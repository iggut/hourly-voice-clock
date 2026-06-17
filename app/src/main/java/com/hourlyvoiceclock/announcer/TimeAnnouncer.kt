package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.local.LocalTtsEngine
import java.time.LocalDateTime

class TimeAnnouncer(
    private val context: Context,
    private val ttsEngine: TtsEngine,
    private val chimePlayer: ChimePlayer,
    private val notifier: AnnouncementNotifier,
    private val hapticPulse: HapticPulse = HapticPulse(context),
    private val ttsConfig: TtsConfigApplier = TtsConfigApplier(ttsEngine),
    private val audioFocusController: AudioFocusController = AudioFocusController(context),
    private val ttsEngineRouter: TtsEngineRouter = TtsEngineRouter(
        primaryEngine = ttsEngine,
        localEngineFactory = { LocalTtsEngine(context) }
    )
) {

    /**
     * How long to wait after the chime finishes and audio focus is
     * granted before asking the TTS engine to start speaking. Without
     * this delay, the audio HAL has not yet finished rerouting to the
     * speech stream when the first frames are dispatched — the engine
     * reports onStart immediately, but the first syllable of the
     * utterance is consumed while the output device is still coming
     * up, producing a clipped announcement. A short settle delay gives
     * the routing layer time to land on the requested stream.
     */
    private val preSpeakSettleMs: Long = 120L

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
                dateTime.dayOfWeek,
                settings.quietDaysQuietStart,
                settings.quietDaysQuietEnd
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
        // Route to the user-selected engine: downloaded on-device voice
        // (LocalTtsEngine) if one is selected and loadable, otherwise
        // the system TTS path.
        val engine = ttsEngineRouter.resolveFor(settings)

        if (!engine.isAvailable()) {
            Log.w(TAG, "TTS not available - attempting init")
        }

        if (engine === ttsEngine) {
            // System TTS path: apply the saved voice/locale/pitch/rate.
            ttsConfig.apply(
                voiceName = settings.selectedVoiceName,
                savedLocale = settings.selectedLocale,
                pitch = settings.pitch,
                speechRate = settings.speechRate,
                audioChannel = settings.audioChannel
            )
        } else {
            // Local engine path: pitch/rate are baked into the model, so
            // only the audio channel needs to be applied.
            engine.setAudioChannel(settings.audioChannel)
        }

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

        // Defer the actual TTS dispatch by a short settle window so the
        // audio HAL has time to finish rerouting to the requested
        // stream before the first samples hit it. Without this, the
        // engine's onStart fires before the output is stable and the
        // opening syllable of the announcement is lost (clipped).
        Handler(Looper.getMainLooper()).postDelayed({
            engine.speakAsync(text) { /* completion handled by engine */ }
        }, preSpeakSettleMs)

        if (settings.notificationLogging) {
            notifier.post(text)
        }
    }

    companion object {
        private const val TAG = "TimeAnnouncer"
    }
}
