package com.k2fsa.sherpa.onnx

class OfflineTtsConfig {
    @JvmField var model: OfflineTtsModelConfig = OfflineTtsModelConfig()
    @JvmField var ruleFsts: String = ""
    @JvmField var ruleFars: String = ""
    @JvmField var maxNumSentences: Int = 1
    @JvmField var silenceScale: Float = 0.2f
}
