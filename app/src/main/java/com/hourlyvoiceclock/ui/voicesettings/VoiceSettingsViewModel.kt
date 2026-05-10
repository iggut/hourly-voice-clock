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

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val ttsRepo = TtsVoiceRepository((application as HourlyVoiceClockApp).ttsEngine)

    private val _normalVoicesByLocale = MutableStateFlow<Map<String, List<VoiceInfo>>>(emptyMap())
    val normalVoicesByLocale: StateFlow<Map<String, List<VoiceInfo>>> = _normalVoicesByLocale.asStateFlow()

    private val _specialVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val specialVoices: StateFlow<List<VoiceInfo>> = _specialVoices.asStateFlow()

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
            _normalVoicesByLocale.value = ttsRepo.getNormalVoicesGroupedByLocale()
            _specialVoices.value = ttsRepo.getSpecialVoices()
            _hasMultipleVoices.value = ttsRepo.hasMultipleVoices()
            val settings = settingsRepo.settings.first()
            _selectedVoiceName.value = settings.selectedVoiceName
            _pitch.value = settings.pitch
            _speechRate.value = settings.speechRate
        }
    }

    fun selectVoice(voiceName: String, localeTag: String) {
        viewModelScope.launch {
            ttsRepo.selectVoice(voiceName, localeTag)
            settingsRepo.setSelectedVoice(voiceName, localeTag)
            _selectedVoiceName.value = voiceName
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
}
