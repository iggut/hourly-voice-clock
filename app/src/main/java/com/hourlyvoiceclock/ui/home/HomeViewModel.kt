package com.hourlyvoiceclock.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.announcer.QuietHoursPolicy
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.tts.AndroidTtsEngine
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val scheduler = AnnouncementScheduler(application)

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _nextAnnouncement = MutableStateFlow("")
    val nextAnnouncement: StateFlow<String> = _nextAnnouncement.asStateFlow()

    private val _quietHoursActive = MutableStateFlow(false)
    val quietHoursActive: StateFlow<Boolean> = _quietHoursActive.asStateFlow()

    private val _hourlyEnabled = MutableStateFlow(false)
    val hourlyEnabled: StateFlow<Boolean> = _hourlyEnabled.asStateFlow()

    private val _canSpeakNow = MutableStateFlow(true)
    val canSpeakNow: StateFlow<Boolean> = _canSpeakNow.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                updateTime()
                delay(1000)
            }
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                _hourlyEnabled.value = settings.hourlyAnnouncementsEnabled
                updateNextAnnouncement(settings.hourlyAnnouncementsEnabled)
                updateQuietStatus(settings)
            }
        }
    }

    private fun updateTime() {
        val now = LocalDateTime.now()
        _currentTime.value = now.format(DateTimeFormatter.ofPattern("h:mm:ss a"))
    }

    private fun updateNextAnnouncement(enabled: Boolean) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            return
        }
        val next = AnnouncementScheduler.getNextTopOfHour()
        _nextAnnouncement.value = next.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    private fun updateQuietStatus(settings: com.hourlyvoiceclock.data.AppSettings) {
        val now = java.time.LocalTime.now()
        val inQuiet = QuietHoursPolicy.isQuietTime(
            now,
            settings.quietHoursEnabled,
            settings.quietHoursStart,
            settings.quietHoursEnd
        )
        _quietHoursActive.value = inQuiet
        _canSpeakNow.value = QuietHoursPolicy.canAnnounceManually(
            now,
            settings.quietHoursEnabled,
            settings.quietHoursStart,
            settings.quietHoursEnd,
            settings.allowManualDuringQuiet
        )
    }

    fun toggleHourly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setHourlyAnnouncements(enabled)
            val settings = settingsRepo.settings.first()
            if (enabled) {
                scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
            } else {
                scheduler.cancelHourlyAlarms()
            }
        }
    }

    fun announceNow(includeDate: Boolean = false) {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val ttsEngine = AndroidTtsEngine(getApplication())
            val ttsRepo = TtsVoiceRepository(ttsEngine)
            val announcer = TimeAnnouncer(getApplication(), ttsRepo)
            try {
                announcer.announce(settings, force = true, includeDate = includeDate)
            } finally {
                ttsEngine.shutdown()
            }
        }
    }
}
