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
    val category: VoiceCategory = VoiceCategory.STANDARD,
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
        ),

        // ----------------------------------------------------------------
        // Curated community voice pack
        //
        // Sourced from the user's local pack at
        //   ~/Downloads/piper_community_voice_pack/manifest.json
        //
        // Each entry below mirrors one row of that manifest. The slugs
        // match the manifest's `slug` field verbatim so the on-disk
        // model directories line up with the registry id and so future
        // regenerations of the manifest stay in sync.
        //
        // Licensing note: many of these voices imitate recognisable
        // fictional characters or performers, and several repos do not
        // clearly establish source-media rights. They are listed for
        // private/local testing. The description on each entry stays
        // neutral and points at the original source repo so the user
        // can make a usage decision of their own.
        //
        // Sizes are best-effort estimates (the manifest does not ship
        // content-lengths). They are used only for the "X MB" label in
        // the Local Voices screen; the downloader uses the live HTTP
        // Content-Length for progress, not this field.
        // ----------------------------------------------------------------

        VoiceModel(
            id = "jarvis_high",
            displayName = "JARVIS high",
            description = "Polished Home Assistant-style British AI assistant voice (community). Source: huggingface.co/jgkawell/jarvis. MIT-licensed repository; for personal use unless rights are verified.",
            language = "en-GB",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/high/jarvis-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/high/jarvis-high.onnx.json",
            onnxFileName = "jarvis-high.onnx",
            onnxJsonFileName = "jarvis-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "jarvis_medium_mk1",
            displayName = "JARVIS medium mk1",
            description = "Lighter medium British AI assistant voice (community). Source: huggingface.co/jgkawell/jarvis. MIT-licensed repository.",
            language = "en-GB",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvismk1/medium/en_GB-jarvis-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvismk1/medium/en_GB-jarvis-medium.onnx.json",
            onnxFileName = "en_GB-jarvis-medium.onnx",
            onnxJsonFileName = "en_GB-jarvis-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "hal_9000_campwill",
            displayName = "HAL 9000 (campwill)",
            description = "Calm AI / sci-fi assistant voice (community). Source: huggingface.co/campwill/HAL-9000-Piper-TTS. Apache-2.0 repository. Imitates a recognisable fictional character; use for private testing unless you have rights.",
            language = "en-US",
            sizeBytes = 30_000_000,
            onnxDownloadUrl = "https://huggingface.co/campwill/HAL-9000-Piper-TTS/resolve/main/hal.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/campwill/HAL-9000-Piper-TTS/resolve/main/hal.onnx.json",
            onnxFileName = "hal.onnx",
            onnxJsonFileName = "hal.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "glados_high",
            displayName = "GLaDOS high",
            description = "Robot / game-AI style voice (community). Source: huggingface.co/csukuangfj/vits-piper-en_US-glados-high. Repository license unclear; imitates a recognisable character. Personal-testing only unless rights are verified.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://huggingface.co/csukuangfj/vits-piper-en_US-glados-high/resolve/main/en_US-glados-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/csukuangfj/vits-piper-en_US-glados-high/resolve/main/en_US-glados-high.onnx.json",
            onnxFileName = "en_US-glados-high.onnx",
            onnxJsonFileName = "en_US-glados-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "hal_9000_denoised_dividebysandwich",
            displayName = "HAL 9000 denoised",
            description = "Alternative sci-fi assistant voice with denoising applied (community). Source: github.com/dividebysandwich/piper-voice-models. Repository does not clearly establish source-media rights; imitates a recognisable character. Personal-testing only.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/HAL9000-denoised/en_US-hal_6409-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/HAL9000-denoised/en_US-hal_6409-medium.onnx.json",
            onnxFileName = "en_US-hal_6409-medium.onnx",
            onnxJsonFileName = "en_US-hal_6409-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "data_dividebysandwich",
            displayName = "Commander Data style",
            description = "Android / sci-fi style voice (community). Source: github.com/dividebysandwich/piper-voice-models. Repository does not clearly establish source-media rights; imitates a recognisable character. Personal-testing only.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Data/en_US-data_7024-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Data/en_US-data_7024-medium.onnx.json",
            onnxFileName = "en_US-data_7024-medium.onnx",
            onnxJsonFileName = "en_US-data_7024-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "picard_dividebysandwich",
            displayName = "Captain Picard style",
            description = "Captain / narration style voice (community). Source: github.com/dividebysandwich/piper-voice-models. Repository does not clearly establish source-media rights; likely identifiable-character/performer likeness. Personal-testing only.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Picard/en_US-picard_7399-medium.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/dividebysandwich/piper-voice-models/main/Picard/en_US-picard_7399-medium.onnx.json",
            onnxFileName = "en_US-picard_7399-medium.onnx",
            onnxJsonFileName = "en_US-picard_7399-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "k9",
            displayName = "K9",
            description = "Compact robot style voice (community). Source: github.com/hopkira/k9_piper_voice. Repository has a LICENSE file; verify terms before redistribution.",
            language = "en-US",
            sizeBytes = 20_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/hopkira/k9_piper_voice/main/k9_model.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/hopkira/k9_piper_voice/main/k9_model.onnx.json",
            onnxFileName = "k9_model.onnx",
            onnxJsonFileName = "k9_model.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "bmo",
            displayName = "BMO",
            description = "Small cute robot style voice (community). Source: github.com/1liminal1/xiaozhi-esphome. Character-likeness rights unclear; personal-testing only.",
            language = "en-US",
            sizeBytes = 30_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/1liminal1/xiaozhi-esphome/main/piper-voices/en_US-bmo_voice.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/1liminal1/xiaozhi-esphome/main/piper-voices/en_US-bmo_voice.onnx.json",
            onnxFileName = "en_US-bmo_voice.onnx",
            onnxJsonFileName = "en_US-bmo_voice.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "bt7274",
            displayName = "BT-7274",
            description = "Mech / tactical AI style voice (community). Source: github.com/DJMalachite/PiperVoiceModels. Game-character rights unclear; personal-testing only.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/DJMalachite/PiperVoiceModels/main/Titanfall2/BT7274/BT7274.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/DJMalachite/PiperVoiceModels/main/Titanfall2/BT7274/BT7274.onnx.json",
            onnxFileName = "BT7274.onnx",
            onnxJsonFileName = "BT7274.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "overwatch_dispatch",
            displayName = "Overwatch dispatch style",
            description = "Dispatch / announcer style voice (community). Source: github.com/robit-man/combine_overwatch_onnx. Repository claims synthetic data/fair use; verify before public use.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://raw.githubusercontent.com/robit-man/combine_overwatch_onnx/main/overwatch.onnx",
            onnxJsonDownloadUrl = "https://raw.githubusercontent.com/robit-man/combine_overwatch_onnx/main/overwatch.onnx.json",
            onnxFileName = "overwatch.onnx",
            onnxJsonFileName = "overwatch.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "subnautica_pda",
            displayName = "Subnautica PDA style",
            description = "PDA / system-alert style voice (community). Source: huggingface.co/Aquaaa123/piper-tts-pda-subnautica. Game-character rights unclear.",
            language = "en-US",
            sizeBytes = 30_000_000,
            onnxDownloadUrl = "https://huggingface.co/Aquaaa123/piper-tts-pda-subnautica/resolve/main/pda.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/Aquaaa123/piper-tts-pda-subnautica/resolve/main/pda.onnx.json",
            onnxFileName = "pda.onnx",
            onnxJsonFileName = "pda.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "kronk",
            displayName = "Kronk style",
            description = "Cartoon / comedic style voice (community). Source: huggingface.co/russdill/kronk. Repository license unclear; character-likeness rights unclear.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://huggingface.co/russdill/kronk/resolve/main/en/en_US/kronk/medium/kronk-medium.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/russdill/kronk/resolve/main/en/en_US/kronk/medium/kronk-medium.onnx.json",
            onnxFileName = "kronk-medium.onnx",
            onnxJsonFileName = "kronk-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "rocket_racoon",
            displayName = "Rocket Raccoon style",
            description = "Raspy character voice (community). Source: huggingface.co/AkumaVenom/RocketRacoon-Piper-US-Medium. Repository license unclear; strong character/performer-likeness concerns. Personal-testing only.",
            language = "en-US",
            sizeBytes = 63_000_000,
            onnxDownloadUrl = "https://huggingface.co/AkumaVenom/RocketRacoon-Piper-US-Medium/resolve/main/rocket_racoon.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/AkumaVenom/RocketRacoon-Piper-US-Medium/resolve/main/rocket_racoon.onnx.json",
            onnxFileName = "rocket_racoon.onnx",
            onnxJsonFileName = "rocket_racoon.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "official_en_us_ryan_high",
            displayName = "Official Piper Ryan high",
            description = "Clean American English male baseline (official rhasspy/piper-voices). MIT-licensed; safe to use. Useful as a quality reference and for time announcements.",
            language = "en-US",
            sizeBytes = 100_000_000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ryan/high/en_US-ryan-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ryan/high/en_US-ryan-high.onnx.json",
            onnxFileName = "en_US-ryan-high.onnx",
            onnxJsonFileName = "en_US-ryan-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),
        VoiceModel(
            id = "official_en_us_ljspeech_high",
            displayName = "Official Piper LJSpeech high",
            description = "Clean American English female baseline (official rhasspy/piper-voices). MIT-licensed; safe to use. Classic LJSpeech-trained voice.",
            language = "en-US",
            sizeBytes = 100_000_000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ljspeech/high/en_US-ljspeech-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/ljspeech/high/en_US-ljspeech-high.onnx.json",
            onnxFileName = "en_US-ljspeech-high.onnx",
            onnxJsonFileName = "en_US-ljspeech-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),
        VoiceModel(
            id = "official_en_us_libritts_high",
            displayName = "Official Piper LibriTTS high",
            description = "Multi-speaker American English baseline (official rhasspy/piper-voices). MIT-licensed; safe to use. High quality reference voice.",
            language = "en-US",
            sizeBytes = 100_000_000,
            onnxDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/libritts/high/en_US-libritts-high.onnx",
            onnxJsonDownloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/libritts/high/en_US-libritts-high.onnx.json",
            onnxFileName = "en_US-libritts-high.onnx",
            onnxJsonFileName = "en_US-libritts-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.STANDARD
        ),

        // ----------------------------------------------------------------
        // simoniz0r/piper-voice-models (GitHub releases)
        //
        // Sourced from https://github.com/simoniz0r/piper-voice-models/releases.
        // Each release ships two assets: `en_US-<name>-medium.onnx` and the
        // matching `en_US-<name>-medium.onnx.json`. Asset names were
        // verified against the GitHub Releases API; URLs were confirmed
        // to return HTTP 200.
        //
        // Character-likeness rights are unclear for several of these
        // voices (Eminem, Bobby, Patrick); descriptions stay neutral and
        // mark them as personal-testing-only.
        // ----------------------------------------------------------------

        VoiceModel(
            id = "simoniz0r_bobby_medium",
            displayName = "Bobby (simoniz0r)",
            description = "American English medium voice (community). Source: github.com/simoniz0r/piper-voice-models. Repository does not clearly establish source-media rights; personal-testing only.",
            language = "en-US",
            sizeBytes = 63_516_050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/bobby/en_US-bobby-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/bobby/en_US-bobby-medium.onnx.json",
            onnxFileName = "en_US-bobby-medium.onnx",
            onnxJsonFileName = "en_US-bobby-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "simoniz0r_carl_medium",
            displayName = "Carl (simoniz0r)",
            description = "American English medium voice (community). Source: github.com/simoniz0r/piper-voice-models. Repository license unclear; personal-testing only.",
            language = "en-US",
            sizeBytes = 63_516_050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/carl/en_US-carl-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/carl/en_US-carl-medium.onnx.json",
            onnxFileName = "en_US-carl-medium.onnx",
            onnxJsonFileName = "en_US-carl-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "simoniz0r_eminem_medium",
            displayName = "Eminem (simoniz0r)",
            description = "American English medium voice (community). Source: github.com/simoniz0r/piper-voice-models. Repository does not clearly establish source-media rights; strong performer-likeness concerns. Personal-testing only.",
            language = "en-US",
            sizeBytes = 63_516_050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/eminem/en_US-eminem-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/eminem/en_US-eminem-medium.onnx.json",
            onnxFileName = "en_US-eminem-medium.onnx",
            onnxJsonFileName = "en_US-eminem-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),
        VoiceModel(
            id = "simoniz0r_patrick_medium",
            displayName = "Patrick (simoniz0r)",
            description = "American English medium voice (community). Source: github.com/simoniz0r/piper-voice-models. Repository does not clearly establish source-media rights; personal-testing only.",
            language = "en-US",
            sizeBytes = 63_516_050,
            onnxDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/patrick/en_US-patrick-medium.onnx",
            onnxJsonDownloadUrl = "https://github.com/simoniz0r/piper-voice-models/releases/download/patrick/en_US-patrick-medium.onnx.json",
            onnxFileName = "en_US-patrick-medium.onnx",
            onnxJsonFileName = "en_US-patrick-medium.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER
        ),

        // ----------------------------------------------------------------
        // BibEBobberson/Piper (HuggingFace model repo)
        //
        // Sourced from https://huggingface.co/BibEBobberson/Piper.
        // The repo ships `Donald Trump.tar.gz`, `George-Carlin.tar.gz`,
        // and `Homer.zip` at the top level. Each tar.gz contains a
        // Piper `en_US-<name>-high.onnx` + `en_US-<name>-high.onnx.json`
        // pair (verified locally — the .onnx is a valid PyTorch-exported
        // ONNX protobuf and the .onnx.json has a populated
        // phoneme_id_map + espeak voice "en-us").
        //
        // Homer.zip is a Coqui/TTS PyTorch checkpoint (`.ckpt` + `.pth`)
        // and reference audio, NOT a Piper ONNX voice. It cannot be
        // loaded by Sherpa-ONNX, so it is intentionally omitted. If a
        // Piper-format Homer is later added upstream, it'll ship then.
        //
        // Trump/Carlin ship as compressed archives; the downloader
        // streams the archive to a temp file and extracts the inner
        // .onnx + .onnx.json.
        //
        // Likeness rights are obvious for both — descriptions stay
        // neutral and the entries are marked personal-testing-only.
        // ----------------------------------------------------------------

        VoiceModel(
            id = "bibebobberson_trump_high",
            displayName = "Trump (BibEBobberson)",
            description = "American English high voice (community). Source: huggingface.co/BibEBobberson/Piper. Shipped as a .tar.gz archive; downloader extracts the .onnx + .onnx.json. Strong performer-likeness concerns. Personal-testing only.",
            language = "en-US",
            sizeBytes = 109_000_000,
            onnxDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/Donald%20Trump.tar.gz",
            onnxJsonDownloadUrl = "",
            onnxFileName = "en_US-trump-high.onnx",
            onnxJsonFileName = "en_US-trump-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            archiveDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/Donald%20Trump.tar.gz"
        ),
        VoiceModel(
            id = "bibebobberson_carlin_high",
            displayName = "George Carlin (BibEBobberson)",
            description = "American English high voice (community). Source: huggingface.co/BibEBobberson/Piper. Shipped as a .tar.gz archive; downloader extracts the .onnx + .onnx.json. Strong performer-likeness concerns. Personal-testing only.",
            language = "en-US",
            sizeBytes = 109_000_000,
            onnxDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/George-Carlin.tar.gz",
            onnxJsonDownloadUrl = "",
            onnxFileName = "en_US-carlin-high.onnx",
            onnxJsonFileName = "en_US-carlin-high.onnx.json",
            sampleRate = 22050,
            category = VoiceCategory.CHARACTER,
            archiveDownloadUrl = "https://huggingface.co/BibEBobberson/Piper/resolve/main/George-Carlin.tar.gz"
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
