package com.hourlyvoiceclock.tts.local

data class VoiceModel(
    val id: String,
    val displayName: String,
    val description: String,
    val language: String,
    val sizeBytes: Long,
    val onnxDownloadUrl: String,
    val onnxJsonDownloadUrl: String,
    val onnxFileName: String,
    val onnxJsonFileName: String,
    val sampleRate: Int = 22050,
    val category: VoiceCategory = VoiceCategory.STANDARD
)

enum class VoiceCategory {
    STANDARD,
    CHARACTER,
    NARRATOR,
    ACCENT
}

/**
 * Catalog of Piper voices hosted on the HuggingFace `rhasspy/piper-voices`
 * mirror. Each entry resolves both the .onnx model file and its sibling
 * .onnx.json (audio config + phoneme id map) so Sherpa-ONNX can load it.
 *
 * Size values are the actual HF content-lengths as of 2026-06-15 and are
 * used only for display; the downloader's "already downloaded" check is
 * existence-based, not size-based, so registry drift is harmless.
 */
object VoiceModelRegistry {

    private const val HF_BASE = "https://huggingface.co/rhasspy/piper-voices/resolve/main"

    val availableVoices: List<VoiceModel> = listOf(
        VoiceModel(
            id = "piper_en_us_amy_medium",
            displayName = "Amy (Friendly)",
            description = "Warm, friendly American English female voice",
            language = "en-US",
            sizeBytes = 63_201_294,
            onnxDownloadUrl = "$HF_BASE/en/en_US/amy/medium/en_US-amy-medium.onnx",
            onnxJsonDownloadUrl = "$HF_BASE/en/en_US/amy/medium/en_US-amy-medium.onnx.json",
            onnxFileName = "en_US-amy-medium.onnx",
            onnxJsonFileName = "en_US-amy-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),
        VoiceModel(
            id = "piper_en_us_lessac_medium",
            displayName = "Lessac (Expressive)",
            description = "Expressive American English male voice with natural intonation",
            language = "en-US",
            sizeBytes = 63_201_294,
            onnxDownloadUrl = "$HF_BASE/en/en_US/lessac/medium/en_US-lessac-medium.onnx",
            onnxJsonDownloadUrl = "$HF_BASE/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json",
            onnxFileName = "en_US-lessac-medium.onnx",
            onnxJsonFileName = "en_US-lessac-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),
        VoiceModel(
            id = "piper_en_us_libritts_r_medium",
            displayName = "LibriTTS (Narrator)",
            description = "Storytelling voice perfect for announcements",
            language = "en-US",
            sizeBytes = 78_580_914,
            onnxDownloadUrl = "$HF_BASE/en/en_US/libritts_r/medium/en_US-libritts_r-medium.onnx",
            onnxJsonDownloadUrl = "$HF_BASE/en/en_US/libritts_r/medium/en_US-libritts_r-medium.onnx.json",
            onnxFileName = "en_US-libritts_r-medium.onnx",
            onnxJsonFileName = "en_US-libritts_r-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.NARRATOR
        ),
        VoiceModel(
            id = "piper_en_gb_alba_medium",
            displayName = "Alba (British)",
            description = "Elegant British English female voice",
            language = "en-GB",
            sizeBytes = 63_201_294,
            onnxDownloadUrl = "$HF_BASE/en/en_GB/alba/medium/en_GB-alba-medium.onnx",
            onnxJsonDownloadUrl = "$HF_BASE/en/en_GB/alba/medium/en_GB-alba-medium.onnx.json",
            onnxFileName = "en_GB-alba-medium.onnx",
            onnxJsonFileName = "en_GB-alba-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.ACCENT
        ),
        VoiceModel(
            id = "piper_en_us_arctic_medium",
            displayName = "Arctic (Character)",
            description = "Deep, commanding voice for dramatic time announcements",
            language = "en-US",
            sizeBytes = 76_766_385,
            onnxDownloadUrl = "$HF_BASE/en/en_US/arctic/medium/en_US-arctic-medium.onnx",
            onnxJsonDownloadUrl = "$HF_BASE/en/en_US/arctic/medium/en_US-arctic-medium.onnx.json",
            onnxFileName = "en_US-arctic-medium.onnx",
            onnxJsonFileName = "en_US-arctic-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "piper_en_us_kusal_medium",
            displayName = "Kusal (Upbeat)",
            description = "Upbeat, energetic American English male voice",
            language = "en-US",
            sizeBytes = 63_201_294,
            onnxDownloadUrl = "$HF_BASE/en/en_US/kusal/medium/en_US-kusal-medium.onnx",
            onnxJsonDownloadUrl = "$HF_BASE/en/en_US/kusal/medium/en_US-kusal-medium.onnx.json",
            onnxFileName = "en_US-kusal-medium.onnx",
            onnxJsonFileName = "en_US-kusal-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        )
    )

    fun getVoiceById(id: String): VoiceModel? =
        availableVoices.find { it.id == id }

    fun getVoicesByCategory(category: VoiceCategory): List<VoiceModel> =
        availableVoices.filter { it.category == category }

    fun formatSize(sizeBytes: Long): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
    }
}
