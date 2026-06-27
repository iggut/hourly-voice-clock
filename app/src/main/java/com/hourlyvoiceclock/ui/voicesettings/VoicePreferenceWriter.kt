package com.hourlyvoiceclock.ui.voicesettings

import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.VoiceInfo

/**
 * Owns the "save a user preference" fan-out for the voice-settings
 * screen: apply to the live [TtsEngine], then persist to the
 * [SettingsRepository].
 *
 * The previous code repeated `engine.set…` + `repo.set…` + mirror to
 * a local `MutableStateFlow` 5-6 times verbatim inside
 * [VoiceSettingsViewModel]; the ViewModel is left with only the
 * mirror-to-StateFlow step.
 *
 * A writer is a [com.hourlyvoiceclock.ui.voicesettings.VoiceSettingsViewModel]
 * collaborator, not a replacement for it — the ViewModel still owns the
 * StateFlows that drive the UI. The writer removes the boilerplate of
 * the dual-write.
 */
class VoicePreferenceWriter(
    private val engine: TtsEngine,
    private val repo: SettingsRepository
) {

    suspend fun setVoice(voiceName: String, localeTag: String) {
        engine.setVoice(voiceName, localeTag)
        repo.update {
            it.copy(
                selectedVoiceName = voiceName,
                selectedLocale = localeTag,
                selectedVoicePresetId = null
            )
        }
    }

    suspend fun setPitch(value: Float) {
        engine.setPitch(value)
        repo.update { it.copy(pitch = value) }
    }

    suspend fun setSpeechRate(value: Float) {
        engine.setSpeechRate(value)
        repo.update { it.copy(speechRate = value) }
    }

    /**
     * Apply a [SpecialVoicePreset] to both the live engine and the
     * repository in one operation. The chosen [voice] is the underlying
     * Android voice the preset selects — see
     * [VoiceSettingsViewModel.choosePresetVoice] for the matching logic.
     */
    suspend fun applyPreset(preset: SpecialVoicePreset, voice: VoiceInfo) {
        engine.setVoice(voice.name, voice.localeTag)
        engine.setPitch(preset.pitch)
        engine.setSpeechRate(preset.speechRate)
        repo.update {
            it.copy(
                selectedVoiceName = voice.name,
                selectedLocale = voice.localeTag,
                selectedVoicePresetId = preset.id,
                pitch = preset.pitch,
                speechRate = preset.speechRate
            )
        }
    }
}
