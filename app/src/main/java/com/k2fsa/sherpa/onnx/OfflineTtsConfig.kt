package com.k2fsa.sherpa.onnx

/**
 * Stub override of the AAR's `OfflineTtsConfig`. The AAR's data
 * class is missing several constructors that the TtsKt factory invokes
 * via the default-arguments bitmask, so we re-declare the class here
 * (in the same package) to inject the needed <init> signatures.
 */
class OfflineTtsConfig(
    @JvmField var model: OfflineTtsModelConfig = OfflineTtsModelConfig(),
    @JvmField var ruleFsts: String = "",
    @JvmField var ruleFars: String = "",
    @JvmField var maxNumSentences: Int = 1,
    @JvmField var silenceScale: Float = 0.2f,
)
