package com.hourlyvoiceclock.tts.local

import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig

class NativeTtsBridge(private val ptr: Long) {
    val sampleRate: Int
        get() = if (ptr != 0L) nativeSampleRate(ptr) else 0

    fun generate(text: String, sid: Int = 0, speed: Float = 1.0f): FloatArray? {
        return if (ptr != 0L) nativeGenerate(ptr, text, sid, speed) else null
    }

    fun destroy() {
        if (ptr != 0L) nativeDestroy(ptr)
    }

    companion object {
        init {
            System.loadLibrary("native-tts-bridge")
        }

        fun create(modelPath: String, tokensPath: String, dataDir: String): NativeTtsBridge? {
            val config = OfflineTtsConfig().apply {
                model = OfflineTtsModelConfig().apply {
                    vits = OfflineTtsVitsModelConfig().apply {
                        this.model = modelPath
                        this.tokens = tokensPath
                        this.dataDir = dataDir
                        lengthScale = 1.0f
                    }
                }
            }
            val ptr = nativeCreate(config)
            return if (ptr != 0L) NativeTtsBridge(ptr) else null
        }

        @JvmStatic
        external fun nativeCreate(config: OfflineTtsConfig): Long

        @JvmStatic
        external fun nativeDestroy(ptr: Long)

        @JvmStatic
        external fun nativeSampleRate(ptr: Long): Int

        @JvmStatic
        external fun nativeGenerate(ptr: Long, text: String, sid: Int, speed: Float): FloatArray?
    }
}
