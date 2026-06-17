package com.k2fsa.sherpa.onnx

/**
 * Stub override of the AAR's `OfflineTtsVitsModelConfig`. The AAR's data
 * class is missing several constructors that the TtsKt factory invokes
 * via the default-arguments bitmask, so we re-declare the class here
 * (in the same package) to inject the needed <init> signatures.
 *
 * Kotlin's data class generates both:
 *   - Primary 8-arg constructor:  (model, lexicon, tokens, dataDir,
 *                                  dictDir, noiseScale, noiseScaleW,
 *                                  lengthScale)
 *   - Synthetic default-args 10-arg constructor: (same 8 args, bitmask,
 *                                  DefaultConstructorMarker)
 *
 * Both must exist or the TtsKt.getOfflineTtsConfig factory throws
 * NoSuchMethodError when initializing a VITS model.
 */
class OfflineTtsVitsModelConfig(
    @JvmField var model: String = "",
    @JvmField var lexicon: String = "",
    @JvmField var tokens: String = "",
    @JvmField var dataDir: String = "",
    @JvmField var dictDir: String = "",
    @JvmField var noiseScale: Float = 0.667f,
    @JvmField var noiseScaleW: Float = 0.8f,
    @JvmField var lengthScale: Float = 1.0f,
)
