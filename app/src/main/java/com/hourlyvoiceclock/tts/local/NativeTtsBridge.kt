package com.hourlyvoiceclock.tts.local

class NativeTtsBridge(private val ptr: Long) {
    val sampleRate: Int
        get() = nativeSampleRate(ptr)

    fun generate(text: String, sid: Int = 0, speed: Float = 1.0f): FloatArray? {
        return nativeGenerate(ptr, text, sid, speed)
    }

    fun destroy() {
        nativeDestroy(ptr)
    }

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-c-api")
            System.loadLibrary("sherpa-onnx-jni")
            System.loadLibrary("native-tts-bridge")
        }

        @JvmStatic
        external fun nativeCreate(modelPath: String, tokensPath: String, dataDir: String): Long

        @JvmStatic
        external fun nativeDestroy(ptr: Long)

        @JvmStatic
        external fun nativeSampleRate(ptr: Long): Int

        @JvmStatic
        external fun nativeGenerate(ptr: Long, text: String, sid: Int, speed: Float): FloatArray?
    }
}
