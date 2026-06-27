package com.hourlyvoiceclock.ui.voicesettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.tts.TtsEngineInfo
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.local.LocalTtsEngine
import com.hourlyvoiceclock.tts.local.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val writer = VoicePreferenceWriter(
        engine = deps.ttsEngine,
        repo = deps.settingsRepository
    )

    private var allNormalVoices = emptyList<VoiceInfo>()

    private val _normalVoicesByLocale = MutableStateFlow<Map<String, List<VoiceInfo>>>(emptyMap())
    val normalVoicesByLocale: StateFlow<Map<String, List<VoiceInfo>>> = _normalVoicesByLocale.asStateFlow()

    private val _selectedFilter = MutableStateFlow(VoiceListFilter.ALL)
    val selectedFilter: StateFlow<VoiceListFilter> = _selectedFilter.asStateFlow()

    val selectedPresetId: StateFlow<String?> = deps.settingsRepository.settings
        .map { it.selectedVoicePresetId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isEspeakNgSelected = MutableStateFlow(false)
    val isEspeakNgSelected: StateFlow<Boolean> = _isEspeakNgSelected.asStateFlow()

    val activeSpecialPresets: List<SpecialVoicePreset>
        get() = buildList {
            addAll(SPECIAL_VOICE_PRESETS)
            if (_isEspeakNgSelected.value) {
                addAll(ESpeakNgVoiceVariants)
            }
        }

    val selectedVoiceName: StateFlow<String?> = deps.settingsRepository.settings
        .map { it.selectedVoiceName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Pitch and speech rate are kept as local MutableStateFlows so the
    // sliders feel responsive; each drag commits to the repository.
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

    /**
     * On-device Piper voices the user has downloaded. Surfaced in the
     * "Voices" list so the user can pick a local voice as the active
     * hourly-announcement voice (which then routes through
     * [LocalTtsEngine] in the announcer).
     *
     * Backed by the app-wide [com.hourlyvoiceclock.tts.local.LocalVoicesStore]
     * so the local-voices screen can publish updates (after a
     * download or delete) and the main voice screen sees them
     * without having to re-scan the disk.
     */
    val downloadedLocalModels: StateFlow<List<VoiceModel>> =
        deps.localVoicesStore.downloadedModels

    /**
     * Currently-selected downloaded on-device voice id, or `null` if
     * the user is using the system TTS path.
     */
    val selectedLocalModelId: StateFlow<String?> = deps.settingsRepository.settings
        .map { it.selectedLocalModelId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Lightweight probe so the voice settings screen can read which
     * local models exist on disk without forcing the (heavy) full
     * Sherpa-ONNX native init.
     */
    private val localTtsProbe = LocalTtsEngine(getApplication<Application>())

    init {
        viewModelScope.launch {
            deps.settingsRepository.runMigrations()
            val selectedPackage = deps.ttsEngineSelector.select()
            deps.ttsEngine.initialize(selectedPackage)

            val settings = deps.settingsRepository.settings.first()
            _selectedEnginePackage.value = settings.selectedTtsEnginePackage
                ?: deps.ttsEngine.getEngines().firstOrNull { it.isInstalled }?.packageName
            _isEspeakNgSelected.value = _selectedEnginePackage.value?.contains("espeak", ignoreCase = true) == true
            _pitch.value = settings.pitch
            _speechRate.value = settings.speechRate

            _engines.value = deps.ttsEngine.getEngines()
            allNormalVoices = deps.ttsEngine.getVoices()
            _hasMultipleVoices.value = allNormalVoices.size > 1

            reconcileStaleVoiceSelection(settings.selectedVoiceName, settings.selectedLocale)
            updateFilteredVoices()

            refreshDownloadedLocalModels()
        }
    }

    /**
     * Re-scan the on-device voice directory and publish the result
     * to the app-wide [com.hourlyvoiceclock.tts.local.LocalVoicesStore]
     * so every observer (the voice-settings screen, the local-voices
     * screen) sees the same list.
     */
    fun refreshDownloadedLocalModels() {
        viewModelScope.launch {
            val models = withContext(Dispatchers.IO) {
                localTtsProbe.getInstalledModels()
            }
            deps.localVoicesStore.setDownloadedModels(models)
        }
    }

    /**
     * Select a downloaded on-device voice as the active hourly
     * announcement voice. Persists the choice and clears any
     * previously selected system-voice fields so the new selection
     * is unambiguous.
     */
    fun selectLocalModel(model: VoiceModel) {
        viewModelScope.launch {
            deps.settingsRepository.update {
                it.copy(
                    selectedLocalModelId = model.id,
                    selectedVoiceName = null,
                    selectedLocale = null,
                    selectedVoicePresetId = null
                )
            }
        }
    }

    /**
     * Clear the on-device voice selection and fall back to the system
     * TTS path. The next announcement will use the engine and voice
     * stored in [AppSettings.selectedTtsEnginePackage] /
     * [AppSettings.selectedVoiceName].
     */
    fun clearLocalModelSelection() {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(selectedLocalModelId = null) }
        }
    }

    /**
     * Preview a downloaded on-device voice by routing through the
     * [LocalTtsEngine] probe. This is a fire-and-forget preview used
     * by the voice list rows next to each downloaded model.
     */
    fun previewLocalModel(model: VoiceModel, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val initialized = withContext(Dispatchers.IO) {
                localTtsProbe.initialize(model.id)
            }
            if (!initialized) {
                onError("Preview failed: model files are not loadable. Try deleting and re-downloading.")
                return@launch
            }
            withContext(Dispatchers.IO) {
                localTtsProbe.speakAsync("Hello from Hourly Voice Clock") { ok ->
                    if (!ok) onError("Preview failed: TTS engine could not synthesize audio")
                }
            }
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
    }

    fun selectVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            writer.setVoice(voiceName, localeTag)
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
            writer.applyPreset(preset, underlyingVoice)
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
            writer.setPitch(value)
            _pitch.value = value
        }
    }

    fun setSpeechRate(value: Float) {
        viewModelScope.launch {
            writer.setSpeechRate(value)
            _speechRate.value = value
        }
    }

    fun previewVoice() {
        viewModelScope.launch {
            runCatching {
                if (!deps.ttsEngine.isAvailable()) {
                    deps.ttsEngine.initialize(_selectedEnginePackage.value)
                }
                deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
            }.onFailure { t ->
                // Keep the screen alive even if the engine is in a bad state.
                // The UI already reflects selection and settings separately.
            }
        }
    }

    fun selectAndPreviewVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            runCatching {
                writer.setVoice(voiceName, localeTag)
                deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
            }
        }
    }

    fun selectAndPreviewPreset(preset: SpecialVoicePreset) {
        viewModelScope.launch {
            runCatching {
                applyPreset(preset)
                deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
            }
        }
    }
}
