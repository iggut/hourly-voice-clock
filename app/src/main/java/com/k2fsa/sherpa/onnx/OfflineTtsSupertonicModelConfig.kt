package com.k2fsa.sherpa.onnx

class OfflineTtsSupertonicModelConfig {
    @JvmField var durationPredictor: String = ""
    @JvmField var textEncoder: String = ""
    @JvmField var vectorEstimator: String = ""
    @JvmField var vocoder: String = ""
    @JvmField var ttsJson: String = ""
    @JvmField var unicodeIndexer: String = ""
    @JvmField var voiceStyle: String = ""
}
