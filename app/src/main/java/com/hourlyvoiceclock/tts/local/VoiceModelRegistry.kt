package com.hourlyvoiceclock.tts.local

data class VoiceModel(
    val id: String,
    val displayName: String,
    val description: String,
    val language: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val fileName: String,
    val sampleRate: Int = 22050,
    val category: VoiceCategory = VoiceCategory.STANDARD
)

enum class VoiceCategory {
    STANDARD,
    CHARACTER,
    NARRATOR,
    ACCENT
}

object VoiceModelRegistry {

    val availableVoices: List<VoiceModel> = listOf(
        VoiceModel(
            id = "piper_en_us_amy_medium",
            displayName = "Amy (Friendly)",
            description = "Warm, friendly American English female voice",
            language = "en-US",
            sizeBytes = 32_000_000,
            downloadUrl = "https://github.com/rhasspy/piper/releases/download/v2.0.0/en_US-amy-medium.onnx",
            fileName = "en_US-amy-medium.onnx",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),
        VoiceModel(
            id = "piper_en_us_lessac_medium",
            displayName = "Lessac (Expressive)",
            description = "Expressive American English male voice with natural intonation",
            language = "en-US",
            sizeBytes = 35_000_000,
            downloadUrl = "https://github.com/rhasspy/piper/releases/download/v2.0.0/en_US-lessac-medium.onnx",
            fileName = "en_US-lessac-medium.onnx",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),
        VoiceModel(
            id = "piper_en_us_libritts_r_medium",
            displayName = "LibriTTS (Narrator)",
            description = "Storytelling voice perfect for announcements",
            language = "en-US",
            sizeBytes = 38_000_000,
            downloadUrl = "https://github.com/rhasspy/piper/releases/download/v2.0.0/en_US-libritts_r-medium.onnx",
            fileName = "en_US-libritts_r-medium.onnx",
            sampleRate = 22050,
            category = VoiceCategory.NARRATOR
        ),
        VoiceModel(
            id = "piper_en_gb_alba_medium",
            displayName = "Alba (British)",
            description = "Elegant British English female voice",
            language = "en-GB",
            sizeBytes = 33_000_000,
            downloadUrl = "https://github.com/rhasspy/piper/releases/download/v2.0.0/en_GB-alba-medium.onnx",
            fileName = "en_GB-alba-medium.onnx",
            sampleRate = 22050,
            category = VoiceCategory.ACCENT
        ),
        VoiceModel(
            id = "piper_en_us_arctic_medium",
            displayName = "Arctic (Character)",
            description = "Deep, commanding voice for dramatic time announcements",
            language = "en-US",
            sizeBytes = 30_000_000,
            downloadUrl = "https://github.com/rhasspy/piper/releases/download/v2.0.0/en_US-arctic-medium.onnx",
            fileName = "en_US-arctic-medium.onnx",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "piper_en_us_kusal_medium",
            displayName = "Kusal (Upbeat)",
            description = "Upbeat, energetic American English male voice",
            language = "en-US",
            sizeBytes = 31_000_000,
            downloadUrl = "https://github.com/rhasspy/piper/releases/download/v2.0.0/en_US-kusal-medium.onnx",
            fileName = "en_US-kusal-medium.onnx",
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
