package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Requests transient audio focus for a spoken announcement, then
 * releases it after a fixed delay.
 *
 * Centralises the SDK 26+ AudioFocusRequest API vs the deprecated
 * [AudioManager.requestAudioFocus] 3-arg form for older devices, plus
 * the main-thread [Handler] that releases the focus. The announcer
 * used to inline all of this inside speakText().
 *
 * The release is scheduled at construction time (after [releaseDelayMs])
 * to match the legacy behaviour: the focus is granted for the duration
 * of a typical announcement, then released regardless of when speech
 * actually finishes. Calling [Held.release] early cancels the scheduled
 * release and abandons the focus immediately.
 *
 * @param context any [Context]; no long-lived references are kept.
 * @param releaseDelayMs how long to hold focus before releasing if the
 *   caller does not release early. The legacy behaviour was 5 seconds.
 */
open class AudioFocusController(
    private val context: Context,
    private val releaseDelayMs: Long = DEFAULT_RELEASE_DELAY_MS
) {

    /**
     * Acquire audio focus with the given [usage] and [audioStream].
     * Returns a non-null [Held] handle that abandons the focus after
     * [releaseDelayMs], or earlier if [Held.release] is called. Returns
     * `null` only if the device has no [AudioManager] service.
     */
    open fun acquire(usage: Int, audioStream: Int): Held? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return null
        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
        } else {
            null
        }

        if (focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                audioStream,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        return Held(audioManager, focusRequest, releaseDelayMs)
    }

    /**
     * Handle to a held audio-focus grant. Auto-releases after
     * [releaseDelayMs]; [release] is idempotent and releases early
     * if not yet released.
     */
    class Held internal constructor(
        private val audioManager: AudioManager,
        private val focusRequest: AudioFocusRequest?,
        releaseDelayMs: Long
    ) {
        private var released = false
        private val mainHandler = Handler(Looper.getMainLooper())
        private val pending = Runnable { release() }

        init {
            mainHandler.postDelayed(pending, releaseDelayMs)
        }

        fun release() {
            if (released) return
            released = true
            mainHandler.removeCallbacks(pending)
            if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
    }

    companion object {
        const val DEFAULT_RELEASE_DELAY_MS = 5000L
    }
}
