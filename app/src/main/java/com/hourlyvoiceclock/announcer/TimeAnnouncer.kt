package com.hourlyvoiceclock.announcer

import android.util.Log
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.VoiceProfile

/**
 * Orchestrates an hourly announcement: policy checks, chime, haptic,
 * audio focus, TTS dispatch, and optional status notification.
 *
 * All Android framework dependencies are injected as ports so the
 * sequencing logic can be unit-tested with fakes.
 */
class TimeAnnouncer(
    private val ttsEngine: TtsEngine,
    private val chimePlayer: ChimePlayer,
    private val notifier: AnnouncementNotifier,
    private val hapticPulse: HapticPulse,
    private val audioFocusController: AudioFocusController,
    private val ttsEngineRouter: TtsEngineRouter,
    private val volumeChecker: VolumeChecker,
    private val userFeedback: UserFeedback,
    private val delayScheduler: DelayScheduler,
    private val preSpeakSettleMs: Long = DEFAULT_PRE_SPEAK_SETTLE_MS
) {

    fun announce(
        settings: AppSettings,
        force: Boolean = false,
        includeDate: Boolean = false,
        dateTime: java.time.LocalDateTime? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val effectiveDateTime = dateTime
            ?: java.time.LocalDateTime.now()
        announceAt(
            settings = settings,
            force = force,
            includeDate = includeDate,
            dateTime = effectiveDateTime,
            onComplete = onComplete
        )
    }

    /**
     * Test-accessible entry point that accepts a deterministic [dateTime]
     * so the [DelayScheduler] and completion callbacks can be advanced
     * without real wall-clock time.
     */
    internal fun announceAt(
        settings: AppSettings,
        force: Boolean = false,
        includeDate: Boolean = false,
        dateTime: java.time.LocalDateTime,
        onComplete: (Boolean) -> Unit
    ) {
        if (AnnouncementPolicy.isBlockedByQuietHours(settings, dateTime, force)) {
            Log.d(TAG, "Blocked by quiet hours")
            onComplete(false)
            return
        }

        val channelSpec = AudioChannelMapping.specOf(settings.audioChannel)
        val audioStream = channelSpec.stream
        val usage = channelSpec.usage

        if (volumeChecker.isMuted(audioStream)) {
            userFeedback.showMutedStreamMessage(channelSpec.shortLabel)
            Log.w(TAG, "Stream $audioStream volume is 0 - cannot hear TTS")
            onComplete(false)
            return
        }
        Log.d(TAG, "Stream $audioStream volume: ${volumeChecker.currentVolume(audioStream)} / ${volumeChecker.maxVolume(audioStream)}")

        if (settings.vibrateBefore) {
            hapticPulse.pulse()
        }

        if (settings.chimeSound != ChimeSound.NONE) {
            chimePlayer.play(settings.chimeSound) {
                speakText(settings, dateTime, includeDate, usage, audioStream, onComplete)
            }
        } else {
            speakText(settings, dateTime, includeDate, usage, audioStream, onComplete)
        }
    }

    private fun speakText(
        settings: AppSettings,
        dateTime: java.time.LocalDateTime,
        includeDate: Boolean,
        usage: Int,
        audioStream: Int,
        onComplete: (Boolean) -> Unit
    ) {
        val engine = ttsEngineRouter.resolveFor(settings)

        if (!engine.isAvailable()) {
            Log.w(TAG, "TTS not available - attempting init")
        }

        engine.configure(
            VoiceProfile(
                voiceName = settings.selectedVoiceName,
                localeTag = settings.selectedLocale,
                localModelId = settings.selectedLocalModelId,
                pitch = settings.pitch,
                speechRate = settings.speechRate,
                audioChannel = settings.audioChannel
            )
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

        // Defer the actual TTS dispatch by a short settle window so the
        // audio HAL has time to finish rerouting to the requested
        // stream before the first samples hit it.
        delayScheduler.schedule(preSpeakSettleMs) {
            engine.speakAsync(text) { success -> onComplete(success) }
        }

        if (settings.notificationLogging) {
            notifier.post(text)
        }
    }

    companion object {
        private const val TAG = "TimeAnnouncer"
        const val DEFAULT_PRE_SPEAK_SETTLE_MS = 120L
    }
}
