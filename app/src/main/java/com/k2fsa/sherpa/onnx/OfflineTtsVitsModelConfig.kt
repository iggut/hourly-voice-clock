package com.k2fsa.sherpa.onnx

class OfflineTtsVitsModelConfig {
    @JvmField var model: String = ""
    @JvmField var lexicon: String = ""
    @JvmField var tokens: String = ""
    @JvmField var dataDir: String = ""
    @JvmField var dictDir: String = ""
    @JvmField var noiseScale: Float = 0.667f
    @JvmField var noiseScaleW: Float = 0.8f
    @JvmField var lengthScale: Float = 1.0f
}
