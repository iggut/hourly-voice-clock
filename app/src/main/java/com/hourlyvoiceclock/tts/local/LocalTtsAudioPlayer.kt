package com.hourlyvoiceclock.tts.local

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.hourlyvoiceclock.announcer.AudioChannelMapping
import com.hourlyvoiceclock.data.AudioChannel
import kotlinx.coroutines.delay

/**
 * Port for playing synthesized PCM audio.
 */
interface LocalTtsAudioPlayer {
    suspend fun play(samples: FloatArray, sampleRate: Int, channel: AudioChannel)
}

/**
 * Production implementation using [AudioTrack] streaming.
 */
class AudioTrackPlayer : LocalTtsAudioPlayer {

    override suspend fun play(samples: FloatArray, sampleRate: Int, channel: AudioChannel) {
        val spec = AudioChannelMapping.specOf(channel)

        // ⚡ Bolt: Use ENCODING_PCM_FLOAT to avoid manual float-to-short conversion overhead
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        // ⚡ Bolt: Float is 4 bytes, so we need samples.size * 4 bytes for the buffer size
        val bufferSize = maxOf(minBuffer, samples.size * 4)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(spec.usage)
                    .setContentType(spec.contentType)
                    .build()
            )
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            track.play()
            // ⚡ Bolt: Directly pass FloatArray to AudioTrack.write() avoiding an intermediate ShortArray allocation and CPU-heavy loop
            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            val playbackDurationMs = (samples.size.toLong() * 1000) / sampleRate
            delay(playbackDurationMs + 50)
        } finally {
            try {
                track.stop()
            } catch (_: IllegalStateException) {
                // already stopped
            }
            track.release()
        }
    }
}
