package com.hourlyvoiceclock.tts.local

import android.content.Context
import androidx.annotation.StringRes
import com.hourlyvoiceclock.R

data class VoiceModel(
    val id: String,
    @StringRes val displayNameRes: Int,
    @StringRes val descriptionRes: Int,
    val language: String,
    val sizeBytes: Long,
    val onnxDownloadUrl: String,
    val onnxJsonDownloadUrl: String,
    val onnxFileName: String,
    val onnxJsonFileName: String,
    val sampleRate: Int = 22050,
    val category: VoiceCategory = VoiceCategory.STANDARD,
    val sourceKind: VoiceSourceKind = VoiceSourceKind.OFFICIAL,
    val personalTestingOnly: Boolean = false,
    /**
     * Optional archive (.zip, .tar.gz, .tgz) URL that, when set, takes
     * precedence over the direct `onnxDownloadUrl` / `onnxJsonDownloadUrl`
     * pair. The downloader streams the archive to a temp file, extracts
     * the first `.onnx` model plus its matching `.onnx.json` (or
     * `config.json`), writes them under the canonical names
     * `onnxFileName` / `onnxJsonFileName`, then continues the usual
     * `tokens.txt` + validation pipeline.
     */
    val archiveDownloadUrl: String? = null
)

fun VoiceModel.displayName(context: Context): String = context.getString(displayNameRes)

fun VoiceModel.description(context: Context): String = context.getString(descriptionRes)

enum class VoiceCategory {
    STANDARD,
    CHARACTER,
    NARRATOR,
    ACCENT
}

enum class VoiceSourceKind {
    OFFICIAL,
    COMMUNITY
}

/**
 * Catalog of Piper voices: official `rhasspy/piper-voices` plus curated
 * community character voices. Community entries are marked
 * [VoiceModel.personalTestingOnly] for clearer in-app labeling.
 *
 * Size values are display-only; the downloader uses HTTP Content-Length.
 */
object VoiceModelRegistry {

    val availableVoices: List<VoiceModel> = listOf(
        VoiceModel(
            id = "piper_en_us_amy_medium",
            displayNameRes = R.string.voice_piper_en_us_amy_medium_name,
            descriptionRes = R.string.voice_piper_en_us_amy_medium_desc,
            language = "en-US",
            sizeBytes = 63201294,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx.json",
            onnxFileName = "en_US-amy-medium.onnx",
            onnxJsonFileName = "en_US-amy-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_lessac_medium",
            displayNameRes = R.string.voice_piper_en_us_lessac_medium_name,
            descriptionRes = R.string.voice_piper_en_us_lessac_medium_desc,
            language = "en-US",
            sizeBytes = 63201294,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json",
            onnxFileName = "en_US-lessac-medium.onnx",
            onnxJsonFileName = "en_US-lessac-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_libritts_r_medium",
            displayNameRes = R.string.voice_piper_en_us_libritts_r_medium_name,
            descriptionRes = R.string.voice_piper_en_us_libritts_r_medium_desc,
            language = "en-US",
            sizeBytes = 78580914,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/libritts_r/medium/en_US-libritts_r-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/libritts_r/medium/en_US-libritts_r-medium.onnx.json",
            onnxFileName = "en_US-libritts_r-medium.onnx",
            onnxJsonFileName = "en_US-libritts_r-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.NARRATOR,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_gb_alba_medium",
            displayNameRes = R.string.voice_piper_en_gb_alba_medium_name,
            descriptionRes = R.string.voice_piper_en_gb_alba_medium_desc,
            language = "en-GB",
            sizeBytes = 63201294,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/alba/medium/en_GB-alba-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/alba/medium/en_GB-alba-medium.onnx.json",
            onnxFileName = "en_GB-alba-medium.onnx",
            onnxJsonFileName = "en_GB-alba-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.ACCENT,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_arctic_medium",
            displayNameRes = R.string.voice_piper_en_us_arctic_medium_name,
            descriptionRes = R.string.voice_piper_en_us_arctic_medium_desc,
            language = "en-US",
            sizeBytes = 76766385,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/arctic/medium/en_US-arctic-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/arctic/medium/en_US-arctic-medium.onnx.json",
            onnxFileName = "en_US-arctic-medium.onnx",
            onnxJsonFileName = "en_US-arctic-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_kusal_medium",
            displayNameRes = R.string.voice_piper_en_us_kusal_medium_name,
            descriptionRes = R.string.voice_piper_en_us_kusal_medium_desc,
            language = "en-US",
            sizeBytes = 63201294,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/kusal/medium/en_US-kusal-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/kusal/medium/en_US-kusal-medium.onnx.json",
            onnxFileName = "en_US-kusal-medium.onnx",
            onnxJsonFileName = "en_US-kusal-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_hfc_female_medium",
            displayNameRes = R.string.voice_piper_en_us_hfc_female_medium_name,
            descriptionRes = R.string.voice_piper_en_us_hfc_female_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/hfc_female/medium/en_US-hfc_female-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/hfc_female/medium/en_US-hfc_female-medium.onnx.json",
            onnxFileName = "en_US-hfc_female-medium.onnx",
            onnxJsonFileName = "en_US-hfc_female-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_hfc_male_medium",
            displayNameRes = R.string.voice_piper_en_us_hfc_male_medium_name,
            descriptionRes = R.string.voice_piper_en_us_hfc_male_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/hfc_male/medium/en_US-hfc_male-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/hfc_male/medium/en_US-hfc_male-medium.onnx.json",
            onnxFileName = "en_US-hfc_male-medium.onnx",
            onnxJsonFileName = "en_US-hfc_male-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_joe_medium",
            displayNameRes = R.string.voice_piper_en_us_joe_medium_name,
            descriptionRes = R.string.voice_piper_en_us_joe_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/joe/medium/en_US-joe-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/joe/medium/en_US-joe-medium.onnx.json",
            onnxFileName = "en_US-joe-medium.onnx",
            onnxJsonFileName = "en_US-joe-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_john_medium",
            displayNameRes = R.string.voice_piper_en_us_john_medium_name,
            descriptionRes = R.string.voice_piper_en_us_john_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/john/medium/en_US-john-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/john/medium/en_US-john-medium.onnx.json",
            onnxFileName = "en_US-john-medium.onnx",
            onnxJsonFileName = "en_US-john-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_kristin_medium",
            displayNameRes = R.string.voice_piper_en_us_kristin_medium_name,
            descriptionRes = R.string.voice_piper_en_us_kristin_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/kristin/medium/en_US-kristin-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/kristin/medium/en_US-kristin-medium.onnx.json",
            onnxFileName = "en_US-kristin-medium.onnx",
            onnxJsonFileName = "en_US-kristin-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_bryce_medium",
            displayNameRes = R.string.voice_piper_en_us_bryce_medium_name,
            descriptionRes = R.string.voice_piper_en_us_bryce_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/bryce/medium/en_US-bryce-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/bryce/medium/en_US-bryce-medium.onnx.json",
            onnxFileName = "en_US-bryce-medium.onnx",
            onnxJsonFileName = "en_US-bryce-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_norman_medium",
            displayNameRes = R.string.voice_piper_en_us_norman_medium_name,
            descriptionRes = R.string.voice_piper_en_us_norman_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/norman/medium/en_US-norman-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/norman/medium/en_US-norman-medium.onnx.json",
            onnxFileName = "en_US-norman-medium.onnx",
            onnxJsonFileName = "en_US-norman-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_sam_medium",
            displayNameRes = R.string.voice_piper_en_us_sam_medium_name,
            descriptionRes = R.string.voice_piper_en_us_sam_medium_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/sam/medium/en_US-sam-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/sam/medium/en_US-sam-medium.onnx.json",
            onnxFileName = "en_US-sam-medium.onnx",
            onnxJsonFileName = "en_US-sam-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_gb_jenny_dioco_medium",
            displayNameRes = R.string.voice_piper_en_gb_jenny_dioco_medium_name,
            descriptionRes = R.string.voice_piper_en_gb_jenny_dioco_medium_desc,
            language = "en-GB",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/jenny_dioco/medium/en_GB-jenny_dioco-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/jenny_dioco/medium/en_GB-jenny_dioco-medium.onnx.json",
            onnxFileName = "en_GB-jenny_dioco-medium.onnx",
            onnxJsonFileName = "en_GB-jenny_dioco-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.ACCENT,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_gb_northern_english_male_medium",
            displayNameRes = R.string.voice_piper_en_gb_northern_english_male_medium_name,
            descriptionRes = R.string.voice_piper_en_gb_northern_english_male_medium_desc,
            language = "en-GB",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/northern_english_male/medium/en_GB-northern_english_male-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/northern_english_male/medium/en_GB-northern_english_male-medium.onnx.json",
            onnxFileName = "en_GB-northern_english_male-medium.onnx",
            onnxJsonFileName = "en_GB-northern_english_male-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.ACCENT,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_gb_vctk_medium",
            displayNameRes = R.string.voice_piper_en_gb_vctk_medium_name,
            descriptionRes = R.string.voice_piper_en_gb_vctk_medium_desc,
            language = "en-GB",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/vctk/medium/en_GB-vctk-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/vctk/medium/en_GB-vctk-medium.onnx.json",
            onnxFileName = "en_GB-vctk-medium.onnx",
            onnxJsonFileName = "en_GB-vctk-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.ACCENT,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "piper_en_us_lessac_high",
            displayNameRes = R.string.voice_piper_en_us_lessac_high_name,
            descriptionRes = R.string.voice_piper_en_us_lessac_high_desc,
            language = "en-US",
            sizeBytes = 100000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/high/en_US-lessac-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/high/en_US-lessac-high.onnx.json",
            onnxFileName = "en_US-lessac-high.onnx",
            onnxJsonFileName = "en_US-lessac-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "jarvis_high",
            displayNameRes = R.string.voice_jarvis_high_name,
            descriptionRes = R.string.voice_jarvis_high_desc,
            language = "en-GB",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/high/jarvis-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/high/jarvis-high.onnx.json",
            onnxFileName = "jarvis-high.onnx",
            onnxJsonFileName = "jarvis-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "jarvis_medium_mk1",
            displayNameRes = R.string.voice_jarvis_medium_mk1_name,
            descriptionRes = R.string.voice_jarvis_medium_mk1_desc,
            language = "en-GB",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvismk1/medium/en_GB-jarvis-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvismk1/medium/en_GB-jarvis-medium.onnx.json",
            onnxFileName = "en_GB-jarvis-medium.onnx",
            onnxJsonFileName = "en_GB-jarvis-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "jarvis_medium",
            displayNameRes = R.string.voice_jarvis_medium_name,
            descriptionRes = R.string.voice_jarvis_medium_desc,
            language = "en-GB",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/medium/jarvis-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/medium/jarvis-medium.onnx.json",
            onnxFileName = "jarvis-medium.onnx",
            onnxJsonFileName = "jarvis-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "hal_9000_campwill",
            displayNameRes = R.string.voice_hal_9000_campwill_name,
            descriptionRes = R.string.voice_hal_9000_campwill_desc,
            language = "en-US",
            sizeBytes = 30000000,
            onnxDownloadUrl = "https://huggingface.co/campwill/HAL-9000-Piper-TTS/resolve/main/hal.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/campwill/HAL-9000-Piper-TTS/resolve/main/hal.onnx.json",
            onnxFileName = "hal.onnx",
            onnxJsonFileName = "hal.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "glados_high",
            displayNameRes = R.string.voice_glados_high_name,
            descriptionRes = R.string.voice_glados_high_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/csukuangfj/vits-piper-en_US-glados-high/resolve/main/en_US-glados-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/csukuangfj/vits-piper-en_US-glados-high/resolve/main/en_US-glados-high.onnx.json",
            onnxFileName = "en_US-glados-high.onnx",
            onnxJsonFileName = "en_US-glados-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "hal_9000_denoised_dividebysandwich",
            displayNameRes = R.string.voice_hal_9000_denoised_dividebysandwich_name,
            descriptionRes = R.string.voice_hal_9000_denoised_dividebysandwich_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/HAL9000-denoised/en_US-hal_6409-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/HAL9000-denoised/en_US-hal_6409-medium.onnx.json",
            onnxFileName = "en_US-hal_6409-medium.onnx",
            onnxJsonFileName = "en_US-hal_6409-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "hal_9000_no_denoise_dividebysandwich",
            displayNameRes = R.string.voice_hal_9000_no_denoise_dividebysandwich_name,
            descriptionRes = R.string.voice_hal_9000_no_denoise_dividebysandwich_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/HAL9000-no-denoise/en_US-hal_12894-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/HAL9000-no-denoise/en_US-hal_12894-medium.onnx.json",
            onnxFileName = "en_US-hal_12894-medium.onnx",
            onnxJsonFileName = "en_US-hal_12894-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "data_dividebysandwich",
            displayNameRes = R.string.voice_data_dividebysandwich_name,
            descriptionRes = R.string.voice_data_dividebysandwich_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Data/en_US-data_7024-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Data/en_US-data_7024-medium.onnx.json",
            onnxFileName = "en_US-data_7024-medium.onnx",
            onnxJsonFileName = "en_US-data_7024-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "picard_dividebysandwich",
            displayNameRes = R.string.voice_picard_dividebysandwich_name,
            descriptionRes = R.string.voice_picard_dividebysandwich_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Picard/en_US-picard_7399-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Picard/en_US-picard_7399-medium.onnx.json",
            onnxFileName = "en_US-picard_7399-medium.onnx",
            onnxJsonFileName = "en_US-picard_7399-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "k9",
            displayNameRes = R.string.voice_k9_name,
            descriptionRes = R.string.voice_k9_desc,
            language = "en-US",
            sizeBytes = 20000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/hopkira/k9_piper_voice/main/k9_model.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/hopkira/k9_piper_voice/main/k9_model.onnx.json",
            onnxFileName = "k9_model.onnx",
            onnxJsonFileName = "k9_model.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "bmo",
            displayNameRes = R.string.voice_bmo_name,
            descriptionRes = R.string.voice_bmo_desc,
            language = "en-US",
            sizeBytes = 30000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/1liminal1/xiaozhi-esphome/main/piper-voices/en_US-bmo_voice.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/1liminal1/xiaozhi-esphome/main/piper-voices/en_US-bmo_voice.onnx.json",
            onnxFileName = "en_US-bmo_voice.onnx",
            onnxJsonFileName = "en_US-bmo_voice.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "bt7274",
            displayNameRes = R.string.voice_bt7274_name,
            descriptionRes = R.string.voice_bt7274_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/DJMalachite/PiperVoiceModels/main/Titanfall2/BT7274/BT7274.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/DJMalachite/PiperVoiceModels/main/Titanfall2/BT7274/BT7274.onnx.json",
            onnxFileName = "BT7274.onnx",
            onnxJsonFileName = "BT7274.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "overwatch_dispatch",
            displayNameRes = R.string.voice_overwatch_dispatch_name,
            descriptionRes = R.string.voice_overwatch_dispatch_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/robit-man/combine_overwatch_onnx/main/overwatch.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/robit-man/combine_overwatch_onnx/main/overwatch.onnx.json",
            onnxFileName = "overwatch.onnx",
            onnxJsonFileName = "overwatch.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "subnautica_pda",
            displayNameRes = R.string.voice_subnautica_pda_name,
            descriptionRes = R.string.voice_subnautica_pda_desc,
            language = "en-US",
            sizeBytes = 30000000,
            onnxDownloadUrl = "https://huggingface.co/Aquaaa123/piper-tts-pda-subnautica/resolve/main/pda.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/Aquaaa123/piper-tts-pda-subnautica/resolve/main/pda.onnx.json",
            onnxFileName = "pda.onnx",
            onnxJsonFileName = "pda.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "kronk",
            displayNameRes = R.string.voice_kronk_name,
            descriptionRes = R.string.voice_kronk_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/russdill/kronk/resolve/main/en/en_US/kronk/medium/kronk-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/russdill/kronk/resolve/main/en/en_US/kronk/medium/kronk-medium.onnx.json",
            onnxFileName = "kronk-medium.onnx",
            onnxJsonFileName = "kronk-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "rocket_racoon",
            displayNameRes = R.string.voice_rocket_racoon_name,
            descriptionRes = R.string.voice_rocket_racoon_desc,
            language = "en-US",
            sizeBytes = 63000000,
            onnxDownloadUrl = "https://huggingface.co/AkumaVenom/RocketRacoon-Piper-US-Medium/resolve/main/rocket_racoon.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/AkumaVenom/RocketRacoon-Piper-US-Medium/resolve/main/rocket_racoon.onnx.json",
            onnxFileName = "rocket_racoon.onnx",
            onnxJsonFileName = "rocket_racoon.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "official_en_us_ryan_high",
            displayNameRes = R.string.voice_official_en_us_ryan_high_name,
            descriptionRes = R.string.voice_official_en_us_ryan_high_desc,
            language = "en-US",
            sizeBytes = 100000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ryan/high/en_US-ryan-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ryan/high/en_US-ryan-high.onnx.json",
            onnxFileName = "en_US-ryan-high.onnx",
            onnxJsonFileName = "en_US-ryan-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "official_en_us_ljspeech_high",
            displayNameRes = R.string.voice_official_en_us_ljspeech_high_name,
            descriptionRes = R.string.voice_official_en_us_ljspeech_high_desc,
            language = "en-US",
            sizeBytes = 100000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ljspeech/high/en_US-ljspeech-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ljspeech/high/en_US-ljspeech-high.onnx.json",
            onnxFileName = "en_US-ljspeech-high.onnx",
            onnxJsonFileName = "en_US-ljspeech-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "official_en_us_libritts_high",
            displayNameRes = R.string.voice_official_en_us_libritts_high_name,
            descriptionRes = R.string.voice_official_en_us_libritts_high_desc,
            language = "en-US",
            sizeBytes = 100000000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/libritts/high/en_US-libritts-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/libritts/high/en_US-libritts-high.onnx.json",
            onnxFileName = "en_US-libritts-high.onnx",
            onnxJsonFileName = "en_US-libritts-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD,
            sourceKind = VoiceSourceKind.OFFICIAL,
            personalTestingOnly = false
        ),
        VoiceModel(
            id = "simoniz0r_bobby_medium",
            displayNameRes = R.string.voice_simoniz0r_bobby_medium_name,
            descriptionRes = R.string.voice_simoniz0r_bobby_medium_desc,
            language = "en-US",
            sizeBytes = 63516050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/bobby/en_US-bobby-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/bobby/en_US-bobby-medium.onnx.json",
            onnxFileName = "en_US-bobby-medium.onnx",
            onnxJsonFileName = "en_US-bobby-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "simoniz0r_carl_medium",
            displayNameRes = R.string.voice_simoniz0r_carl_medium_name,
            descriptionRes = R.string.voice_simoniz0r_carl_medium_desc,
            language = "en-US",
            sizeBytes = 63516050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/carl/en_US-carl-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/carl/en_US-carl-medium.onnx.json",
            onnxFileName = "en_US-carl-medium.onnx",
            onnxJsonFileName = "en_US-carl-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "simoniz0r_eminem_medium",
            displayNameRes = R.string.voice_simoniz0r_eminem_medium_name,
            descriptionRes = R.string.voice_simoniz0r_eminem_medium_desc,
            language = "en-US",
            sizeBytes = 63516050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/eminem/en_US-eminem-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/eminem/en_US-eminem-medium.onnx.json",
            onnxFileName = "en_US-eminem-medium.onnx",
            onnxJsonFileName = "en_US-eminem-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "simoniz0r_patrick_medium",
            displayNameRes = R.string.voice_simoniz0r_patrick_medium_name,
            descriptionRes = R.string.voice_simoniz0r_patrick_medium_desc,
            language = "en-US",
            sizeBytes = 63516050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/patrick/en_US-patrick-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/patrick/en_US-patrick-medium.onnx.json",
            onnxFileName = "en_US-patrick-medium.onnx",
            onnxJsonFileName = "en_US-patrick-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true
        ),
        VoiceModel(
            id = "bibebobberson_trump_high",
            displayNameRes = R.string.voice_bibebobberson_trump_high_name,
            descriptionRes = R.string.voice_bibebobberson_trump_high_desc,
            language = "en-US",
            sizeBytes = 109000000,
            onnxDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/Donald%20Trump.tar.gz",
            onnxJsonDownloadUrl = "",
            onnxFileName = "en_US-trump-high.onnx",
            onnxJsonFileName = "en_US-trump-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true,
            archiveDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/Donald%20Trump.tar.gz"
        ),
        VoiceModel(
            id = "bibebobberson_carlin_high",
            displayNameRes = R.string.voice_bibebobberson_carlin_high_name,
            descriptionRes = R.string.voice_bibebobberson_carlin_high_desc,
            language = "en-US",
            sizeBytes = 109000000,
            onnxDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/George-Carlin.tar.gz",
            onnxJsonDownloadUrl = "",
            onnxFileName = "en_US-carlin-high.onnx",
            onnxJsonFileName = "en_US-carlin-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            sourceKind = VoiceSourceKind.COMMUNITY,
            personalTestingOnly = true,
            archiveDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/George-Carlin.tar.gz"
        ),
    )

    fun getVoiceById(id: String): VoiceModel? =
        availableVoices.find { it.id == id }

    fun getVoicesByCategory(category: VoiceCategory): List<VoiceModel> =
        availableVoices.filter { it.category == category }

    fun formatSize(context: Context, sizeBytes: Long): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            context.getString(R.string.size_mb, mb)
        } else {
            context.getString(R.string.size_kb, kb)
        }
    }
}
