package com.k2fsa.sherpa.onnx

class OfflineTtsKokoroModelConfig {
    @JvmField var model: String = ""
    @JvmField var voices: String = ""
    @JvmField var tokens: String = ""
    @JvmField var lexicon: String = ""
    @JvmField var lang: String = ""
    @JvmField var dataDir: String = ""
    @JvmField var dictDir: String = ""
    @JvmField var lengthScale: Float = 1.0f
}
