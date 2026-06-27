package com.hourlyvoiceclock.ui.formatsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import com.hourlyvoiceclock.di.DependenciesProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FormatSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val deps = (application as DependenciesProvider).dependencies

    val timeFormat: StateFlow<TimeFormat> = deps.settingsRepository.settings
        .map { it.timeFormat }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeFormat.HOUR_12)

    val phraseStyle: StateFlow<PhraseStyle> = deps.settingsRepository.settings
        .map { it.phraseStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PhraseStyle.SIMPLE)

    val chimeSound: StateFlow<ChimeSound> = deps.settingsRepository.settings
        .map { it.chimeSound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChimeSound.NONE)

    val vibrateBefore: StateFlow<Boolean> = deps.settingsRepository.settings
        .map { it.vibrateBefore }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val announceDate: StateFlow<Boolean> = deps.settingsRepository.settings
        .map { it.announceDateOnDemand }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val audioChannel: StateFlow<AudioChannel> = deps.settingsRepository.settings
        .map { it.audioChannel }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioChannel.MEDIA)

    // Text fields keep a local source of truth so keystrokes are reflected
    // immediately; the debounced write persists to the repository.
    private val _customPrefix = MutableStateFlow("It is now ")
    val customPrefix: StateFlow<String> = _customPrefix.asStateFlow()

    private val _customSuffix = MutableStateFlow("")
    val customSuffix: StateFlow<String> = _customSuffix.asStateFlow()

    private var prefixSaveJob: Job? = null
    private var suffixSaveJob: Job? = null

    init {
        viewModelScope.launch {
            deps.settingsRepository.settings.collect { settings ->
                _customPrefix.value = settings.customPrefix
                _customSuffix.value = settings.customSuffix
            }
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(timeFormat = format) }
        }
    }

    fun setPhraseStyle(style: PhraseStyle) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(phraseStyle = style) }
        }
    }

    fun setCustomPrefix(prefix: String) {
        _customPrefix.value = prefix
        prefixSaveJob?.cancel()
        prefixSaveJob = viewModelScope.launch {
            delay(500)
            deps.settingsRepository.update { it.copy(customPrefix = prefix) }
        }
    }

    fun setCustomSuffix(suffix: String) {
        _customSuffix.value = suffix
        suffixSaveJob?.cancel()
        suffixSaveJob = viewModelScope.launch {
            delay(500)
            deps.settingsRepository.update { it.copy(customSuffix = suffix) }
        }
    }

    fun setChimeSound(sound: ChimeSound) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(chimeSound = sound) }
        }
    }

    fun setVibrateBefore(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(vibrateBefore = enabled) }
        }
    }

    fun setAnnounceDate(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(announceDateOnDemand = enabled) }
        }
    }

    fun setAudioChannel(channel: AudioChannel) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(audioChannel = channel) }
        }
    }
}
