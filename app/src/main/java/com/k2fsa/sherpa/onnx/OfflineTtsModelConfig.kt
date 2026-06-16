package com.k2fsa.sherpa.onnx

class OfflineTtsModelConfig {
    @JvmField var vits: OfflineTtsVitsModelConfig = OfflineTtsVitsModelConfig()
    @JvmField var matcha: OfflineTtsMatchaModelConfig = OfflineTtsMatchaModelConfig()
    @JvmField var kokoro: OfflineTtsKokoroModelConfig = OfflineTtsKokoroModelConfig()
    @JvmField var zipvoice: OfflineTtsZipVoiceModelConfig = OfflineTtsZipVoiceModelConfig()
    @JvmField var kitten: OfflineTtsKittenModelConfig = OfflineTtsKittenModelConfig()
    @JvmField var pocket: OfflineTtsPocketModelConfig = OfflineTtsPocketModelConfig()
    @JvmField var supertonic: OfflineTtsSupertonicModelConfig = OfflineTtsSupertonicModelConfig()
    @JvmField var numThreads: Int = 1
    @JvmField var debug: Boolean = false
    @JvmField var provider: String = "cpu"
}
