package com.hourlyvoiceclock.ui.voicesettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.tts.TtsEngineInfo
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.local.VoiceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VoiceListFilter {
    ALL,
    MALE,
    FEMALE,
    SPECIAL
}

enum class SpecialVoiceTag {
    FUN,
    CHARACTER,
    ACCENT,
    ESPEAK
}

data class SpecialVoicePreset(
    val id: String,
    @androidx.annotation.StringRes val nameRes: Int,
    @androidx.annotation.StringRes val descriptionRes: Int,
    val pitch: Float,
    val speechRate: Float,
    val preferredGender: String,
    val preferredLocalePrefixes: List<String> = listOf("en-US", "en"),
    val voiceNameHints: List<String> = emptyList(),
    val prefersNetworkVoice: Boolean? = null,
    val tag: SpecialVoiceTag = SpecialVoiceTag.FUN
)

val SPECIAL_VOICE_PRESETS = listOf(
    SpecialVoicePreset(
        id = "preset_robot",
        nameRes = R.string.preset_robot_name,
        descriptionRes = R.string.preset_robot_desc,
        pitch = 0.55f,
        speechRate = 0.78f,
        preferredGender = "Male",
        voiceNameHints = listOf("robot", "droid", "synth", "male"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_narrator",
        nameRes = R.string.preset_narrator_name,
        descriptionRes = R.string.preset_narrator_desc,
        pitch = 0.9f,
        speechRate = 0.88f,
        preferredGender = "Female",
        preferredLocalePrefixes = listOf("en-US", "en-CA", "en"),
        voiceNameHints = listOf("female", "slt", "clb"),
        prefersNetworkVoice = true,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_giant",
        nameRes = R.string.preset_giant_name,
        descriptionRes = R.string.preset_giant_desc,
        pitch = 0.45f,
        speechRate = 0.68f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-GB", "en-US", "en"),
        voiceNameHints = listOf("male", "deep", "rjs", "gdb"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    ),
    SpecialVoicePreset(
        id = "preset_chipmunk",
        nameRes = R.string.preset_chipmunk_name,
        descriptionRes = R.string.preset_chipmunk_desc,
        pitch = 1.65f,
        speechRate = 1.25f,
        preferredGender = "Female",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("female", "young", "tfg", "slt"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    ),
    SpecialVoicePreset(
        id = "preset_paris",
        nameRes = R.string.preset_paris_name,
        descriptionRes = R.string.preset_paris_desc,
        pitch = 0.98f,
        speechRate = 0.92f,
        preferredGender = "Female",
        preferredLocalePrefixes = listOf("fr-FR", "fr-CA", "fr"),
        voiceNameHints = listOf("female", "fr", "victoire"),
        prefersNetworkVoice = true,
        tag = SpecialVoiceTag.ACCENT
    ),
    SpecialVoicePreset(
        id = "preset_radio",
        nameRes = R.string.preset_radio_name,
        descriptionRes = R.string.preset_radio_desc,
        pitch = 0.72f,
        speechRate = 0.82f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-GB", "en-US", "en"),
        voiceNameHints = listOf("male", "gb", "rjs"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_baby",
        nameRes = R.string.preset_baby_name,
        descriptionRes = R.string.preset_baby_desc,
        pitch = 1.9f,
        speechRate = 1.18f,
        preferredGender = "Female",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("female", "young"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    ),
    SpecialVoicePreset(
        id = "preset_cartoon",
        nameRes = R.string.preset_cartoon_name,
        descriptionRes = R.string.preset_cartoon_desc,
        pitch = 1.75f,
        speechRate = 1.45f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("male", "cartoon"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    ),
    SpecialVoicePreset(
        id = "preset_professor",
        nameRes = R.string.preset_professor_name,
        descriptionRes = R.string.preset_professor_desc,
        pitch = 1.08f,
        speechRate = 0.96f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-GB", "en-US", "en"),
        voiceNameHints = listOf("male", "gb", "gdb"),
        prefersNetworkVoice = true,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_slowmo",
        nameRes = R.string.preset_slowmo_name,
        descriptionRes = R.string.preset_slowmo_desc,
        pitch = 0.85f,
        speechRate = 0.58f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("male", "slow"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    ),
    SpecialVoicePreset(
        id = "preset_whisper",
        nameRes = R.string.preset_whisper_name,
        descriptionRes = R.string.preset_whisper_desc,
        pitch = 1.05f,
        speechRate = 0.72f,
        preferredGender = "Female",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("female", "whisper", "soft", "slt"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_news",
        nameRes = R.string.preset_news_name,
        descriptionRes = R.string.preset_news_desc,
        pitch = 1.0f,
        speechRate = 1.02f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("male", "news", "network"),
        prefersNetworkVoice = true,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_auctioneer",
        nameRes = R.string.preset_auctioneer_name,
        descriptionRes = R.string.preset_auctioneer_desc,
        pitch = 1.15f,
        speechRate = 1.55f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("male", "fast"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    ),
    SpecialVoicePreset(
        id = "preset_drill",
        nameRes = R.string.preset_drill_name,
        descriptionRes = R.string.preset_drill_desc,
        pitch = 0.5f,
        speechRate = 1.12f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("male", "deep", "rjs"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_storyteller",
        nameRes = R.string.preset_storyteller_name,
        descriptionRes = R.string.preset_storyteller_desc,
        pitch = 0.82f,
        speechRate = 0.74f,
        preferredGender = "Male",
        preferredLocalePrefixes = listOf("en-GB", "en-US", "en"),
        voiceNameHints = listOf("male", "gb", "warm"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.CHARACTER
    ),
    SpecialVoicePreset(
        id = "preset_sparkle",
        nameRes = R.string.preset_sparkle_name,
        descriptionRes = R.string.preset_sparkle_desc,
        pitch = 1.55f,
        speechRate = 1.2f,
        preferredGender = "Female",
        preferredLocalePrefixes = listOf("en-US", "en"),
        voiceNameHints = listOf("female", "young", "tfg", "slt"),
        prefersNetworkVoice = false,
        tag = SpecialVoiceTag.FUN
    )
)

// eSpeak NG-specific fun voice variants
val ESpeakNgVoiceVariants = listOf(
    SpecialVoicePreset(
        id = "espeak_robot",
        nameRes = R.string.espeak_robot_name,
        descriptionRes = R.string.espeak_robot_desc,
        pitch = 0.5f,
        speechRate = 0.75f,
        preferredGender = "Male",
        voiceNameHints = listOf("robot", "croak", "klatt"),
        tag = SpecialVoiceTag.ESPEAK
    ),
    SpecialVoicePreset(
        id = "espeak_alien",
        nameRes = R.string.espeak_alien_name,
        descriptionRes = R.string.espeak_alien_desc,
        pitch = 1.5f,
        speechRate = 0.9f,
        preferredGender = "Male",
        voiceNameHints = listOf("whisper", "klatt", "male"),
        tag = SpecialVoiceTag.ESPEAK
    ),
    SpecialVoicePreset(
        id = "espeak_monster",
        nameRes = R.string.espeak_monster_name,
        descriptionRes = R.string.espeak_monster_desc,
        pitch = 0.3f,
        speechRate = 0.6f,
        preferredGender = "Male",
        voiceNameHints = listOf("male", "croak"),
        tag = SpecialVoiceTag.ESPEAK
    ),
    SpecialVoicePreset(
        id = "espeak_cartoon",
        nameRes = R.string.espeak_cartoon_name,
        descriptionRes = R.string.espeak_cartoon_desc,
        pitch = 1.8f,
        speechRate = 1.4f,
        preferredGender = "Female",
        voiceNameHints = listOf("female", "whisper"),
        tag = SpecialVoiceTag.ESPEAK
    ),
    SpecialVoicePreset(
        id = "espeak_deep",
        nameRes = R.string.espeak_deep_name,
        descriptionRes = R.string.espeak_deep_desc,
        pitch = 0.35f,
        speechRate = 0.85f,
        preferredGender = "Male",
        voiceNameHints = listOf("male", "klatt"),
        tag = SpecialVoiceTag.ESPEAK
    ),
    SpecialVoicePreset(
        id = "espeak_ghost",
        nameRes = R.string.espeak_ghost_name,
        descriptionRes = R.string.espeak_ghost_desc,
        pitch = 0.6f,
        speechRate = 0.7f,
        preferredGender = "Female",
        voiceNameHints = listOf("whisper", "female"),
        tag = SpecialVoiceTag.ESPEAK
    )
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

    private val _specialTagFilter = MutableStateFlow<SpecialVoiceTag?>(null)
    val specialTagFilter: StateFlow<SpecialVoiceTag?> = _specialTagFilter.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val selectedPresetId: StateFlow<String?> = deps.settingsRepository.settings
        .map { it.selectedVoicePresetId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isEspeakNgSelected = MutableStateFlow(false)
    val isEspeakNgSelected: StateFlow<Boolean> = _isEspeakNgSelected.asStateFlow()

    val activeSpecialPresets: List<SpecialVoicePreset>
        get() {
            val tag = _specialTagFilter.value
            return buildList {
                addAll(SPECIAL_VOICE_PRESETS)
                if (_isEspeakNgSelected.value) {
                    addAll(ESpeakNgVoiceVariants)
                }
            }.filter { tag == null || it.tag == tag }
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
     * [com.hourlyvoiceclock.tts.local.LocalTtsEngine] in the announcer).
     *
     * Backed by the app-wide [com.hourlyvoiceclock.tts.local.LocalVoiceRepository]
     * so the local-voices screen can publish updates (after a
     * download or delete) and the main voice screen sees them
     * without having to re-scan the disk.
     */
    val downloadedLocalModels: StateFlow<List<VoiceModel>> =
        deps.localVoiceRepository.downloadedModels

    /**
     * Currently-selected downloaded on-device voice id, or `null` if
     * the user is using the system TTS path.
     */
    val selectedLocalModelId: StateFlow<String?> = deps.settingsRepository.settings
        .map { it.selectedLocalModelId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
     * through [com.hourlyvoiceclock.tts.local.LocalVoiceRepository] so
     * every observer sees the same list.
     */
    fun refreshDownloadedLocalModels() {
        viewModelScope.launch {
            deps.localVoiceRepository.refreshDownloadedModels()
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
     * Preview a downloaded on-device voice through the repository.
     * This is a fire-and-forget preview used by the voice list rows
     * next to each downloaded model.
     */
    fun previewLocalModel(model: VoiceModel, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            deps.localVoiceRepository.preview(model) { message ->
                onError(message)
                _userMessage.value = message
            }
        }
    }

    fun setVoiceFilter(filter: VoiceListFilter) {
        _selectedFilter.value = filter
        updateFilteredVoices()
    }

    fun setSpecialTagFilter(tag: SpecialVoiceTag?) {
        _specialTagFilter.value = tag
    }

    fun consumeUserMessage() {
        _userMessage.value = null
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
                        selectedVoicePresetId = null,
                        selectedLocalModelId = null
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

    private suspend fun applyPreset(preset: SpecialVoicePreset): Boolean {
        val underlyingVoice = choosePresetVoice(preset)

        if (underlyingVoice != null) {
            writer.applyPreset(preset, underlyingVoice)
            _pitch.value = preset.pitch
            _speechRate.value = preset.speechRate
            return true
        }

        _userMessage.value = getApplication<Application>().getString(
            R.string.preset_no_matching_voice,
            getApplication<Application>().getString(preset.nameRes)
        )
        return false
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
            val localId = deps.settingsRepository.settings.first().selectedLocalModelId
            if (!localId.isNullOrBlank()) {
                val model = downloadedLocalModels.value.firstOrNull { it.id == localId }
                    ?: com.hourlyvoiceclock.tts.local.VoiceModelRegistry.getVoiceById(localId)
                if (model != null) {
                    deps.localVoiceRepository.preview(model) { message ->
                        _userMessage.value = message
                    }
                    return@launch
                }
                _userMessage.value = getApplication<Application>().getString(R.string.selected_local_voice_missing)
                return@launch
            }

            runCatching {
                if (!deps.ttsEngine.isAvailable()) {
                    deps.ttsEngine.initialize(_selectedEnginePackage.value)
                }
                deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
            }.onFailure {
                _userMessage.value = getApplication<Application>().getString(R.string.preview_failed_tts)
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
                if (applyPreset(preset)) {
                    deps.ttsEngine.speakAsync("The time is 3:45 PM.") { }
                }
            }
        }
    }
}
