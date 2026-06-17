package com.k2fsa.sherpa.onnx

class OfflineTtsPocketModelConfig(
    @JvmField var lmFlow: String = "",
    @JvmField var lmMain: String = "",
    @JvmField var encoder: String = "",
    @JvmField var decoder: String = "",
    @JvmField var textConditioner: String = "",
    @JvmField var vocabJson: String = "",
    @JvmField var tokenScoresJson: String = "",
    @JvmField var voiceEmbeddingCacheCapacity: Int = 0,
)
