package com.hourlyvoiceclock.ui.voicesettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import com.hourlyvoiceclock.tts.VoiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SpecialVoicePreset(
    val id: String,
    val displayName: String,
    val pitch: Float,
    val speechRate: Float,
    val preferredGender: String
)

val SPECIAL_VOICE_PRESETS = listOf(
    SpecialVoicePreset("preset_robot", "Robot", 0.4f, 0.8f, "Male"),
    SpecialVoicePreset("preset_freeman", "Morgan Freeman", 0.82f, 0.85f, "Male"),
    SpecialVoicePreset("preset_giant", "Giant", 0.5f, 0.7f, "Male"),
    SpecialVoicePreset("preset_chipmunk", "Chipmunk", 1.7f, 1.3f, "Female"),
    SpecialVoicePreset("preset_goblin", "Goblin", 1.4f, 1.1f, "Male"),
    SpecialVoicePreset("preset_redneck", "Redneck", 0.8f, 0.9f, "Male"),
    SpecialVoicePreset("preset_baby", "Baby", 2.0f, 1.2f, "Female"),
    SpecialVoicePreset("preset_donald", "Donald Duck", 2.0f, 2.0f, "Male"),
    SpecialVoicePreset("preset_nerdy", "Nerdy", 1.2f, 1.2f, "Male"),
    SpecialVoicePreset("preset_slowmo", "Slow Motion", 0.9f, 0.6f, "Male")
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val ttsRepo = TtsVoiceRepository((application as HourlyVoiceClockApp).ttsEngine)

    private var allNormalVoices = emptyList<VoiceInfo>()

    private val _normalVoicesByLocale = MutableStateFlow<Map<String, List<VoiceInfo>>>(emptyMap())
    val normalVoicesByLocale: StateFlow<Map<String, List<VoiceInfo>>> = _normalVoicesByLocale.asStateFlow()

    private val _selectedGender = MutableStateFlow("All")
    val selectedGender: StateFlow<String> = _selectedGender.asStateFlow()

    val specialPresets = SPECIAL_VOICE_PRESETS

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _hasMultipleVoices = MutableStateFlow(false)
    val hasMultipleVoices: StateFlow<Boolean> = _hasMultipleVoices.asStateFlow()

    init {
        viewModelScope.launch {
            ttsRepo.initialize()
            allNormalVoices = ttsRepo.getAllVoices()
            _hasMultipleVoices.value = allNormalVoices.size > 1
            
            val settings = settingsRepo.settings.first()
            _selectedVoiceName.value = settings.selectedVoiceName
            _pitch.value = settings.pitch
            _speechRate.value = settings.speechRate
            
            updateFilteredVoices()
        }
    }

    fun setGenderFilter(gender: String) {
        _selectedGender.value = gender
        updateFilteredVoices()
    }

    private fun updateFilteredVoices() {
        val gender = _selectedGender.value
        val filtered = if (gender == "All") {
            allNormalVoices
        } else {
            allNormalVoices.filter { it.genderLabel == gender }
        }
        _normalVoicesByLocale.value = filtered.groupBy { it.localeDisplayName }
    }

    fun selectVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            ttsRepo.selectVoice(voiceName, localeTag)
            settingsRepo.setSelectedVoice(voiceName, localeTag)
            _selectedVoiceName.value = voiceName
        }
    }

    fun selectVoicePreset(preset: SpecialVoicePreset) {
        viewModelScope.launch {
            val underlyingVoice = allNormalVoices.find { it.localeTag.startsWith("en-US") && it.genderLabel == preset.preferredGender }
                ?: allNormalVoices.find { it.localeTag.startsWith("en-") }
                ?: allNormalVoices.firstOrNull()

            if (underlyingVoice != null) {
                ttsRepo.selectVoice(underlyingVoice.name, underlyingVoice.localeTag)
                settingsRepo.setSelectedVoice(underlyingVoice.name, underlyingVoice.localeTag)
                _selectedVoiceName.value = underlyingVoice.name

                setPitch(preset.pitch)
                setSpeechRate(preset.speechRate)
            }
        }
    }

    fun setPitch(value: Float) {
        viewModelScope.launch {
            ttsRepo.setPitch(value)
            settingsRepo.setPitch(value)
            _pitch.value = value
        }
    }

    fun setSpeechRate(value: Float) {
        viewModelScope.launch {
            ttsRepo.setSpeechRate(value)
            settingsRepo.setSpeechRate(value)
            _speechRate.value = value
        }
    }

    fun previewVoice() {
        ttsRepo.previewVoice("The time is 3:45 PM.")
    }

    fun selectAndPreviewVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            ttsRepo.selectVoice(voiceName, localeTag)
            settingsRepo.setSelectedVoice(voiceName, localeTag)
            _selectedVoiceName.value = voiceName
            ttsRepo.previewVoice("The time is 3:45 PM.")
        }
    }
}
