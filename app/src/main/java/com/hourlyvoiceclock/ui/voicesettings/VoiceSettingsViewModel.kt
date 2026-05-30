package com.hourlyvoiceclock.ui.voicesettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.TtsEngineInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class VoiceListFilter {
    ALL,
    MALE,
    FEMALE,
    SPECIAL
}

data class SpecialVoicePreset(
    val id: String,
    val displayName: String,
    val pitch: Float,
    val speechRate: Float,
    val preferredGender: String
)

// General voice presets that work with any TTS engine
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

// eSpeak NG-specific fun voice variants
val ESpeakNgVoiceVariants = listOf(
    SpecialVoicePreset("espeak_robot", "Robot", 0.5f, 0.75f, "Male"),
    SpecialVoicePreset("espeak_alien", "Alien", 1.5f, 0.9f, "Male"),
    SpecialVoicePreset("espeak_monster", "Monster", 0.3f, 0.6f, "Male"),
    SpecialVoicePreset("espeak_cartoon", "Cartoon", 1.8f, 1.4f, "Female"),
    SpecialVoicePreset("espeak_deep", "Deep Voice", 0.35f, 0.85f, "Male"),
    SpecialVoicePreset("espeak_chipmunk", "Chipmunk", 1.9f, 1.5f, "Female"),
    SpecialVoicePreset("espeak_ghost", "Ghost", 0.6f, 0.7f, "Female"),
    SpecialVoicePreset("espeak_dwarf", "Dwarf", 0.45f, 0.8f, "Male"),
    SpecialVoicePreset("espeak_evil", "Evil", 0.55f, 1.1f, "Male"),
    SpecialVoicePreset("espeak_wizard", "Wizard", 0.4f, 0.65f, "Male"),
    SpecialVoicePreset("espeak_baby", "Baby", 2.2f, 1.3f, "Female"),
    SpecialVoicePreset("espeak_robot_female", "Robot Female", 0.7f, 0.85f, "Female")
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val ttsRepo = TtsVoiceRepository((application as HourlyVoiceClockApp).ttsEngine)

    private var allNormalVoices = emptyList<VoiceInfo>()

    private val _normalVoicesByLocale = MutableStateFlow<Map<String, List<VoiceInfo>>>(emptyMap())
    val normalVoicesByLocale: StateFlow<Map<String, List<VoiceInfo>>> = _normalVoicesByLocale.asStateFlow()

    private val _selectedFilter = MutableStateFlow(VoiceListFilter.ALL)
    val selectedFilter: StateFlow<VoiceListFilter> = _selectedFilter.asStateFlow()

    private val _selectedPresetId = MutableStateFlow<String?>(null)
    val selectedPresetId: StateFlow<String?> = _selectedPresetId.asStateFlow()

    private val _isEspeakNgSelected = MutableStateFlow(false)
    val isEspeakNgSelected: StateFlow<Boolean> = _isEspeakNgSelected.asStateFlow()

    val activeSpecialPresets: List<SpecialVoicePreset>
        get() = buildList {
            addAll(SPECIAL_VOICE_PRESETS)
            if (_isEspeakNgSelected.value) {
                addAll(ESpeakNgVoiceVariants)
            }
        }

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _hasMultipleVoices = MutableStateFlow(false)
    val hasMultipleVoices: StateFlow<Boolean> = _hasMultipleVoices.asStateFlow()

    private val _engines = MutableStateFlow<List<TtsEngineInfo>>(emptyList())
    val engines: StateFlow<List<TtsEngineInfo>> = _engines.asStateFlow()

    private val _selectedEnginePackage = MutableStateFlow<String?>(null)
    val selectedEnginePackage: StateFlow<String?> = _selectedEnginePackage.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.runMigrations()
            ttsRepo.initialize()

            val settings = settingsRepo.settings.first()
            _selectedEnginePackage.value = settings.selectedTtsEnginePackage
                ?: ttsRepo.getEngines().firstOrNull { it.isInstalled }?.packageName
            _isEspeakNgSelected.value = _selectedEnginePackage.value?.contains("espeak", ignoreCase = true) == true
            _selectedVoiceName.value = settings.selectedVoiceName
            _selectedPresetId.value = settings.selectedVoicePresetId
            _pitch.value = settings.pitch
            _speechRate.value = settings.speechRate

            _engines.value = ttsRepo.getEngines()
            allNormalVoices = ttsRepo.getAllVoices()
            _hasMultipleVoices.value = allNormalVoices.size > 1

            reconcileStaleVoiceSelection(settings.selectedVoiceName, settings.selectedLocale)
            updateFilteredVoices()
        }
    }

    fun setVoiceFilter(filter: VoiceListFilter) {
        _selectedFilter.value = filter
        updateFilteredVoices()
    }

    fun switchTtsEngine(packageName: String) {
        viewModelScope.launch {
            val success = ttsRepo.switchEngine(packageName)
            if (success) {
                settingsRepo.setSelectedTtsEnginePackage(packageName)
                _selectedEnginePackage.value = packageName
                _isEspeakNgSelected.value = packageName.contains("espeak", ignoreCase = true)

                settingsRepo.setSelectedVoice(null, null)
                settingsRepo.setSelectedVoicePreset(null)
                _selectedVoiceName.value = null
                _selectedPresetId.value = null

                _engines.value = ttsRepo.getEngines()
                allNormalVoices = ttsRepo.getAllVoices()
                _hasMultipleVoices.value = allNormalVoices.size > 1

                updateFilteredVoices()
            }
        }
    }

    private fun updateFilteredVoices() {
        if (_selectedFilter.value == VoiceListFilter.SPECIAL) {
            _normalVoicesByLocale.value = emptyMap()
            return
        }

        val genderFilter = when (_selectedFilter.value) {
            VoiceListFilter.MALE -> "Male"
            VoiceListFilter.FEMALE -> "Female"
            else -> null
        }

        val filtered = if (genderFilter == null) {
            allNormalVoices
        } else {
            allNormalVoices.filter { it.genderLabel == genderFilter }
        }
        _normalVoicesByLocale.value = filtered.groupBy { it.localeDisplayName }
    }

    private suspend fun reconcileStaleVoiceSelection(savedVoiceName: String?, savedLocale: String?) {
        if (savedVoiceName.isNullOrBlank()) return

        val voiceStillExists = allNormalVoices.any {
            it.name == savedVoiceName && (savedLocale.isNullOrBlank() || it.localeTag == savedLocale)
        }
        if (voiceStillExists) return

        settingsRepo.setSelectedVoice(null, null)
        settingsRepo.setSelectedVoicePreset(null)
        _selectedVoiceName.value = null
        _selectedPresetId.value = null
    }

    fun selectVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            ttsRepo.selectVoice(voiceName, localeTag)
            settingsRepo.setSelectedVoice(voiceName, localeTag)
            _selectedVoiceName.value = voiceName
            _selectedPresetId.value = null
        }
    }

    fun selectVoicePreset(preset: SpecialVoicePreset) {
        viewModelScope.launch {
            applyPreset(preset)
        }
    }

    private suspend fun applyPreset(preset: SpecialVoicePreset) {
        val underlyingVoice = allNormalVoices.find {
            it.localeTag.startsWith("en-US") && it.genderLabel == preset.preferredGender
        }
            ?: allNormalVoices.find { it.localeTag.startsWith("en-") }
            ?: allNormalVoices.firstOrNull()

        if (underlyingVoice != null) {
            ttsRepo.selectVoice(underlyingVoice.name, underlyingVoice.localeTag)
            settingsRepo.setSelectedVoice(underlyingVoice.name, underlyingVoice.localeTag)
            settingsRepo.setSelectedVoicePreset(preset.id)
            _selectedVoiceName.value = underlyingVoice.name
            _selectedPresetId.value = preset.id

            ttsRepo.setPitch(preset.pitch)
            settingsRepo.setPitch(preset.pitch)
            _pitch.value = preset.pitch

            ttsRepo.setSpeechRate(preset.speechRate)
            settingsRepo.setSpeechRate(preset.speechRate)
            _speechRate.value = preset.speechRate
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
            _selectedPresetId.value = null
            ttsRepo.previewVoice("The time is 3:45 PM.")
        }
    }

    fun selectAndPreviewPreset(preset: SpecialVoicePreset) {
        viewModelScope.launch {
            applyPreset(preset)
            ttsRepo.previewVoice("The time is 3:45 PM.")
        }
    }
}
