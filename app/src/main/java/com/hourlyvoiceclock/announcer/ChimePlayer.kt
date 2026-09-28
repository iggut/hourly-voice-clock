package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import java.util.concurrent.atomic.AtomicBoolean

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
open class ChimePlayer(private val context: Context) {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Play the given [sound]. [onComplete] is invoked on the main thread once
     * playback finishes, fails to start, or is skipped because the sound is
     * [ChimeSound.NONE] or the resource is missing. Never throws.
     */
    open fun play(sound: ChimeSound, onComplete: () -> Unit) {
        play(sound, AudioChannel.NOTIFICATION, onComplete)
    }

    /**
     * Play [sound] on the same output as the spoken announcement.
     *
     * Audio attributes have to be set while the player is still idle.
     * [MediaPlayer.create] returns an already-prepared player, and setting
     * attributes after that is ignored (MediaPlayer logs state 8) so the
     * chime would ignore the user's audio channel.
     */
    open fun play(sound: ChimeSound, channel: AudioChannel, onComplete: () -> Unit) {
        val resourceId = resourceIdFor(sound)
        if (resourceId == 0) {
            if (sound != ChimeSound.NONE) {
                Log.w(TAG, "No resource for chime: $sound")
            }
            onComplete()
            return
        }

        val mediaPlayer = MediaPlayer()
        val finished = AtomicBoolean(false)
        fun finish() {
            if (!finished.compareAndSet(false, true)) return
            // Release on the next loop turn. Releasing inside the completion
            // callback drops a native event and logcat reports
            // "mediaplayer went away with unhandled events".
            mediaPlayer.setOnCompletionListener(null)
            mediaPlayer.setOnErrorListener(null)
            mediaPlayer.setOnPreparedListener(null)
            runCatching {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.reset()
            }
            val player = mediaPlayer
            mainHandler.post {
                runCatching { player.release() }
            }
            onComplete()
        }

        try {
            mediaPlayer.setAudioAttributes(chimeAudioAttributes(channel))
            context.resources.openRawResourceFd(resourceId).use { afd ->
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mediaPlayer.setOnCompletionListener { finish() }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                finish()
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing chime: $sound", e)
            finish()
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

/** Sonification attributes routed through the announcement's audio channel. */
internal fun chimeAudioAttributes(channel: AudioChannel): AudioAttributes {
    val spec = AudioChannelMapping.specOf(channel)
    return AudioAttributes.Builder()
        .setUsage(spec.usage)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
