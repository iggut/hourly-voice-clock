package com.hourlyvoiceclock.tts.local

import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig

/**
 * Port for native Sherpa-ONNX speech synthesis.
 *
 * The interface is intentionally narrow (generate/release/sampleRate) so
 * tests can replace the native engine with a fake.
 */
interface LocalTtsSynthesizer {
    val sampleRate: Int
    fun generate(text: String, speed: Float = 1.0f): GeneratedAudio?
    fun release()
}

/**
 * Production implementation backed by [OfflineTts].
 */
class OfflineTtsSynthesizer(private val tts: OfflineTts) : LocalTtsSynthesizer {

    override val sampleRate: Int get() = tts.sampleRate()

    override fun generate(text: String, speed: Float): GeneratedAudio? {
        return tts.generate(text, 0, speed)
    }

    override fun release() {
        tts.release()
    }
}
