package com.k2fsa.sherpa.onnx

class OfflineTtsZipVoiceModelConfig(
    @JvmField var tokens: String = "",
    @JvmField var encoder: String = "",
    @JvmField var decoder: String = "",
    @JvmField var vocoder: String = "",
    @JvmField var dataDir: String = "",
    @JvmField var lexicon: String = "",
    @JvmField var featScale: Float = 1.0f,
    @JvmField var tShift: Float = 0.0f,
    @JvmField var targetRms: Float = 0.0f,
    @JvmField var guidanceScale: Float = 1.0f,
)
