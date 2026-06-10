package com.hourlyvoiceclock.ui.voicesettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.di.DependenciesProvider
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
    val preferredGender: String,
    val preferredLocalePrefixes: List<String> = listOf("en-US", "en"),
    val voiceNameHints: List<String> = emptyList(),
    val prefersNetworkVoice: Boolean? = null
)

// General voice presets that work with any TTS engine.  These now vary the
// underlying voice/locale as well as pitch/rate so the presets do not collapse
// into a chaotic set of near-identical slider changes.
val SPECIAL_VOICE_PRESETS = listOf(
    SpecialVoicePreset("preset_robot", "Robot", 0.55f, 0.78f, "Male", voiceNameHints = listOf("robot", "droid", "synth", "male"), prefersNetworkVoice = false),
    SpecialVoicePreset("preset_narrator", "Warm Narrator", 0.9f, 0.88f, "Female", listOf("en-US", "en-CA", "en"), listOf("female", "slt", "clb"), prefersNetworkVoice = true),
    SpecialVoicePreset("preset_giant", "Giant", 0.45f, 0.68f, "Male", listOf("en-GB", "en-US", "en"), listOf("male", "deep", "rjs", "gdb"), prefersNetworkVoice = false),
    SpecialVoicePreset("preset_chipmunk", "Chipmunk", 1.65f, 1.25f, "Female", listOf("en-US", "en"), listOf("female", "young", "tfg", "slt"), prefersNetworkVoice = false),
    SpecialVoicePreset("preset_paris", "Paris Café", 0.98f, 0.92f, "Female", listOf("fr-FR", "fr-CA", "fr"), listOf("female", "fr", "victoire"), prefersNetworkVoice = true),
    SpecialVoicePreset("preset_radio", "Old Radio", 0.72f, 0.82f, "Male", listOf("en-GB", "en-US", "en"), listOf("male", "gb", "rjs"), prefersNetworkVoice = false),
    SpecialVoicePreset("preset_baby", "Baby", 1.9f, 1.18f, "Female", listOf("en-US", "en"), listOf("female", "young"), prefersNetworkVoice = false),
    SpecialVoicePreset("preset_cartoon", "Cartoon Duck", 1.75f, 1.45f, "Male", listOf("en-US", "en"), listOf("male", "cartoon"), prefersNetworkVoice = false),
    SpecialVoicePreset("preset_professor", "Professor", 1.08f, 0.96f, "Male", listOf("en-GB", "en-US", "en"), listOf("male", "gb", "gdb"), prefersNetworkVoice = true),
    SpecialVoicePreset("preset_slowmo", "Slow Motion", 0.85f, 0.58f, "Male", listOf("en-US", "en"), listOf("male", "slow"), prefersNetworkVoice = false)
)

// eSpeak NG-specific fun voice variants
val ESpeakNgVoiceVariants = listOf(
    SpecialVoicePreset("espeak_robot", "eSpeak Robot", 0.5f, 0.75f, "Male", voiceNameHints = listOf("robot", "croak", "klatt")),
    SpecialVoicePreset("espeak_alien", "eSpeak Alien", 1.5f, 0.9f, "Male", voiceNameHints = listOf("whisper", "klatt", "male")),
    SpecialVoicePreset("espeak_monster", "eSpeak Monster", 0.3f, 0.6f, "Male", voiceNameHints = listOf("male", "croak")),
    SpecialVoicePreset("espeak_cartoon", "eSpeak Cartoon", 1.8f, 1.4f, "Female", voiceNameHints = listOf("female", "whisper")),
    SpecialVoicePreset("espeak_deep", "eSpeak Deep Voice", 0.35f, 0.85f, "Male", voiceNameHints = listOf("male", "klatt")),
    SpecialVoicePreset("espeak_ghost", "eSpeak Ghost", 0.6f, 0.7f, "Female", voiceNameHints = listOf("whisper", "female"))
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val deps = (application as DependenciesProvider).dependencies

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
            deps.settingsRepository.runMigrations()
            val selectedPackage = deps.ttsEngineSelector.select()
            deps.ttsEngine.initialize(selectedPackage)

            val settings = deps.settingsRepository.settings.first()
            _selectedEnginePackage.value = settings.selectedTtsEnginePackage
                ?: deps.ttsEngine.getEngines().firstOrNull { it.isInstalled }?.packageName
            _isEspeakNgSelected.value = _selectedEnginePackage.value?.contains("espeak", ignoreCase = true) == true
            _selectedVoiceName.value = settings.selectedVoiceName
            _selectedPresetId.value = settings.selectedVoicePresetId
            _pitch.value = settings.pitch
            _speechRate.value = settings.speechRate

            _engines.value = deps.ttsEngine.getEngines()
            allNormalVoices = deps.ttsEngine.getVoices()
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
            val success = deps.ttsEngine.switchEngine(packageName)
            if (success) {
                deps.settingsRepository.update {
                    it.copy(
                        selectedTtsEnginePackage = packageName,
                        selectedVoiceName = null,
                        selectedLocale = null,
                        selectedVoicePresetId = null
                    )
                }
                _selectedEnginePackage.value = packageName
                _isEspeakNgSelected.value = packageName.contains("espeak", ignoreCase = true)
                _selectedVoiceName.value = null
                _selectedPresetId.value = null

                _engines.value = deps.ttsEngine.getEngines()
                allNormalVoices = deps.ttsEngine.getVoices()
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

        deps.settingsRepository.update {
            it.copy(selectedVoiceName = null, selectedLocale = null, selectedVoicePresetId = null)
        }
        _selectedVoiceName.value = null
        _selectedPresetId.value = null
    }

    fun selectVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            deps.ttsEngine.setVoice(voiceName, localeTag)
            deps.settingsRepository.setSelectedVoice(voiceName, localeTag)
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
        val underlyingVoice = choosePresetVoice(preset)

        if (underlyingVoice != null) {
            deps.ttsEngine.setVoice(underlyingVoice.name, underlyingVoice.localeTag)
            deps.ttsEngine.setPitch(preset.pitch)
            deps.ttsEngine.setSpeechRate(preset.speechRate)
            deps.settingsRepository.update {
                it.copy(
                    selectedVoiceName = underlyingVoice.name,
                    selectedLocale = underlyingVoice.localeTag,
                    selectedVoicePresetId = preset.id,
                    pitch = preset.pitch,
                    speechRate = preset.speechRate
                )
            }
            _selectedVoiceName.value = underlyingVoice.name
            _selectedPresetId.value = preset.id
            _pitch.value = preset.pitch
            _speechRate.value = preset.speechRate
        }
    }

    private fun choosePresetVoice(preset: SpecialVoicePreset): VoiceInfo? {
        return preset.preferredLocalePrefixes.asSequence()
            .mapNotNull { prefix -> bestPresetVoice(preset, prefix) }
            .firstOrNull()
            ?: bestPresetVoice(preset, "en")
            ?: bestPresetVoice(preset, "fr")
            ?: allNormalVoices.firstOrNull()
    }

    private fun bestPresetVoice(preset: SpecialVoicePreset, localePrefix: String): VoiceInfo? {
        val candidates = allNormalVoices.filter { it.localeTag.startsWith(localePrefix, ignoreCase = true) }
        if (candidates.isEmpty()) return null

        return candidates.maxWithOrNull(
            compareBy<VoiceInfo> { it.genderLabel == preset.preferredGender }
                .thenBy { preset.prefersNetworkVoice == null || it.requiresNetwork == preset.prefersNetworkVoice }
                .thenBy { voice -> preset.voiceNameHints.any { hint -> voice.name.contains(hint, ignoreCase = true) } }
                .thenBy { it.quality }
                .thenByDescending { it.latency }
        )
    }

    fun setPitch(value: Float) {
        viewModelScope.launch {
            deps.ttsEngine.setPitch(value)
            deps.settingsRepository.setPitch(value)
            _pitch.value = value
        }
    }

    fun setSpeechRate(value: Float) {
        viewModelScope.launch {
            deps.ttsEngine.setSpeechRate(value)
            deps.settingsRepository.setSpeechRate(value)
            _speechRate.value = value
        }
    }

    fun previewVoice() {
        deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
    }

    fun selectAndPreviewVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            deps.ttsEngine.setVoice(voiceName, localeTag)
            deps.settingsRepository.setSelectedVoice(voiceName, localeTag)
            _selectedVoiceName.value = voiceName
            _selectedPresetId.value = null
            deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
        }
    }

    fun selectAndPreviewPreset(preset: SpecialVoicePreset) {
        viewModelScope.launch {
            applyPreset(preset)
            deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
        }
    }
}
