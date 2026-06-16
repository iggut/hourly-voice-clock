package com.k2fsa.sherpa.onnx

class OfflineTtsMatchaModelConfig {
    @JvmField var acousticModel: String = ""
    @JvmField var vocoder: String = ""
    @JvmField var lexicon: String = ""
    @JvmField var tokens: String = ""
    @JvmField var dataDir: String = ""
    @JvmField var dictDir: String = ""
    @JvmField var noiseScale: Float = 0.667f
    @JvmField var lengthScale: Float = 1.0f
}
