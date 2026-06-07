package com.hourlyvoiceclock.ui.formatsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import com.hourlyvoiceclock.di.DependenciesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class FormatSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val deps = (application as DependenciesProvider).dependencies

    private val _timeFormat = MutableStateFlow(TimeFormat.HOUR_12)
    val timeFormat: StateFlow<TimeFormat> = _timeFormat.asStateFlow()

    private val _phraseStyle = MutableStateFlow(PhraseStyle.SIMPLE)
    val phraseStyle: StateFlow<PhraseStyle> = _phraseStyle.asStateFlow()

    private val _customPrefix = MutableStateFlow("It is now ")
    val customPrefix: StateFlow<String> = _customPrefix.asStateFlow()

    private val _customSuffix = MutableStateFlow("")
    val customSuffix: StateFlow<String> = _customSuffix.asStateFlow()

    private val _chimeSound = MutableStateFlow(ChimeSound.NONE)
    val chimeSound: StateFlow<ChimeSound> = _chimeSound.asStateFlow()

    private val _vibrateBefore = MutableStateFlow(false)
    val vibrateBefore: StateFlow<Boolean> = _vibrateBefore.asStateFlow()

    private val _announceDate = MutableStateFlow(false)
    val announceDate: StateFlow<Boolean> = _announceDate.asStateFlow()

    private val _audioChannel = MutableStateFlow(AudioChannel.MEDIA)
    val audioChannel: StateFlow<AudioChannel> = _audioChannel.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = deps.settingsRepository.settings.first()
            _timeFormat.value = settings.timeFormat
            _phraseStyle.value = settings.phraseStyle
            _customPrefix.value = settings.customPrefix
            _customSuffix.value = settings.customSuffix
            _chimeSound.value = settings.chimeSound
            _vibrateBefore.value = settings.vibrateBefore
            _announceDate.value = settings.announceDateOnDemand
            _audioChannel.value = settings.audioChannel
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            deps.settingsRepository.setTimeFormat(format)
            _timeFormat.value = format
        }
    }

    fun setPhraseStyle(style: PhraseStyle) {
        viewModelScope.launch {
            deps.settingsRepository.setPhraseStyle(style)
            _phraseStyle.value = style
        }
    }

    private var prefixSaveJob: Job? = null

    fun setCustomPrefix(prefix: String) {
        _customPrefix.value = prefix
        prefixSaveJob?.cancel()
        prefixSaveJob = viewModelScope.launch {
            delay(500)
            deps.settingsRepository.setCustomPrefix(prefix)
        }
    }

    private var suffixSaveJob: Job? = null

    fun setCustomSuffix(suffix: String) {
        _customSuffix.value = suffix
        suffixSaveJob?.cancel()
        suffixSaveJob = viewModelScope.launch {
            delay(500)
            deps.settingsRepository.setCustomSuffix(suffix)
        }
    }

    fun setChimeSound(sound: ChimeSound) {
        viewModelScope.launch {
            deps.settingsRepository.setChimeSound(sound)
            _chimeSound.value = sound
        }
    }

    fun setVibrateBefore(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.setVibrateBefore(enabled)
            _vibrateBefore.value = enabled
        }
    }

    fun setAnnounceDate(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.setAnnounceDateOnDemand(enabled)
            _announceDate.value = enabled
        }
    }

    fun setAudioChannel(channel: AudioChannel) {
        viewModelScope.launch {
            deps.settingsRepository.setAudioChannel(channel)
            _audioChannel.value = channel
        }
    }
}
