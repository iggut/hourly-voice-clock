package com.k2fsa.sherpa.onnx

class OfflineTtsKittenModelConfig(
    @JvmField var model: String = "",
    @JvmField var voices: String = "",
    @JvmField var tokens: String = "",
    @JvmField var dataDir: String = "",
    @JvmField var lengthScale: Float = 1.0f,
)
