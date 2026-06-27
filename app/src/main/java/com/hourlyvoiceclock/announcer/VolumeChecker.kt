package com.hourlyvoiceclock.announcer

import android.content.Context
import android.media.AudioManager

/**
 * Port for checking the volume state of an audio stream.
 *
 * Abstracting this lets [TimeAnnouncer] stay free of direct Android
 * framework calls and makes the muted-stream path testable with a fake.
 */
interface VolumeChecker {
    fun isMuted(audioStream: Int): Boolean
    fun currentVolume(audioStream: Int): Int
    fun maxVolume(audioStream: Int): Int
}

/**
 * Production implementation backed by the system [AudioManager].
 */
class AndroidVolumeChecker(context: Context) : VolumeChecker {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun isMuted(audioStream: Int): Boolean = currentVolume(audioStream) == 0

    override fun currentVolume(audioStream: Int): Int {
        return audioManager?.getStreamVolume(audioStream) ?: 0
    }

    override fun maxVolume(audioStream: Int): Int {
        return audioManager?.getStreamMaxVolume(audioStream) ?: 1
    }
}
