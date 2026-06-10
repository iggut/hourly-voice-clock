package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.ChimeSound

/**
 * Plays a short audio clip ("chime") before a spoken announcement.
 *
 * Owns the [ChimeSound] → [android.media.MediaPlayer] lifecycle, including
 * release on completion and graceful error handling. The mapping from
 * [ChimeSound] to a `R.raw.*` resource id lives here so it stays next to the
 * player that consumes it.
 *
 * @param context any [Context] (the application context is fine — no long-lived
 *   references are kept).
 */
class ChimePlayer(private val context: Context) {

    /**
     * Play the given [sound]. [onComplete] is invoked on the main thread once
     * playback finishes, fails to start, or is skipped because the sound is
     * [ChimeSound.NONE] or the resource is missing. Never throws.
     */
    fun play(sound: ChimeSound, onComplete: () -> Unit) {
        val resourceId = resourceIdFor(sound)
        if (resourceId == 0) {
            if (sound != ChimeSound.NONE) {
                Log.w(TAG, "No resource for chime: $sound")
            }
            onComplete()
            return
        }

        try {
            val mediaPlayer = MediaPlayer.create(context, resourceId)
            if (mediaPlayer == null) {
                Log.w(TAG, "MediaPlayer.create returned null for $sound")
                onComplete()
                return
            }
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
                onComplete()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing chime: $sound", e)
            onComplete()
        }
    }

    /**
     * Map a [ChimeSound] to its raw resource id, or `0` for [ChimeSound.NONE]
     * or any sound that has no bundled resource. Pure function — exposed
     * (internal) for testability and so callers can branch on availability
     * without invoking the player.
     */
    internal fun resourceIdFor(sound: ChimeSound): Int = when (sound) {
        ChimeSound.NONE -> 0
        ChimeSound.CLASSIC_CHIME -> R.raw.classic_chime
        ChimeSound.BELL -> R.raw.bell
        ChimeSound.GONG -> R.raw.gong
        ChimeSound.CYMBALS -> R.raw.cymbals
        ChimeSound.DIGITAL_BEEP -> R.raw.digital_beep
        ChimeSound.BIRD_CHIRP -> R.raw.bird_chirp
        ChimeSound.HONK -> R.raw.honk
    }

    companion object {
        private const val TAG = "ChimePlayer"
    }
}
