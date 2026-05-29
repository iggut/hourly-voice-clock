package com.hourlyvoiceclock.ui.home

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.announcer.QuietHoursPolicy
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpdateAvailable(val latestVersion: String, val downloadUrl: String, val releaseNotes: String) : UpdateStatus
    object UpToDate : UpdateStatus
    object NoRelease : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val scheduler = AnnouncementScheduler(application)
    private val ttsRepo = TtsVoiceRepository((application as HourlyVoiceClockApp).ttsEngine)
    private val announcer = TimeAnnouncer(application, ttsRepo)

    val appSettings: StateFlow<com.hourlyvoiceclock.data.AppSettings> = settingsRepo.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.hourlyvoiceclock.data.AppSettings()
        )

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _nextAnnouncement = MutableStateFlow("")
    val nextAnnouncement: StateFlow<String> = _nextAnnouncement.asStateFlow()

    private val _quietHoursActive = MutableStateFlow(false)
    val quietHoursActive: StateFlow<Boolean> = _quietHoursActive.asStateFlow()

    private val _hourlyEnabled = MutableStateFlow(false)
    val hourlyEnabled: StateFlow<Boolean> = _hourlyEnabled.asStateFlow()

    private val _canSpeakNow = MutableStateFlow(true)
    val canSpeakNow: StateFlow<Boolean> = _canSpeakNow.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private var autoCheckCompleted = false

    init {
        viewModelScope.launch {
            while (isActive) {
                updateTime()
                if (_hourlyEnabled.value) {
                    updateNextAnnouncement(true)
                }
                delay(1000)
            }
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                _hourlyEnabled.value = settings.hourlyAnnouncementsEnabled
                updateQuietStatus(settings)
                if (!autoCheckCompleted && settings.autoUpdateEnabled) {
                    autoCheckCompleted = true
                    checkForUpdates(isManual = false)
                }
            }
        }
        viewModelScope.launch {
            val initOk = ttsRepo.initialize()
            if (!initOk) {
                Toast.makeText(getApplication(), "Text-to-Speech initialization failed. Check your TTS engine in system settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTime() {
        val now = LocalDateTime.now()
        _currentTime.value = now.format(TIME_FORMATTER)
        _currentDate.value = now.format(DATE_FORMATTER)
    }

    private fun updateNextAnnouncement(enabled: Boolean) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            return
        }
        val next = AnnouncementScheduler.getNextTopOfHour()
        _nextAnnouncement.value = next.format(NEXT_ANNOUNCEMENT_FORMATTER)
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
            announcer.announce(settings, force = true, includeDate = includeDate)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAutoUpdateEnabled(enabled)
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val currentVersion = try {
                val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                pInfo.versionName ?: "0.1"
            } catch (e: Exception) {
                "0.1"
            }

            com.hourlyvoiceclock.data.UpdateChecker.checkForUpdate(currentVersion)
                .onSuccess { info ->
                    if (info.isUpdateAvailable) {
                        _updateStatus.value = UpdateStatus.UpdateAvailable(
                            latestVersion = info.latestVersion,
                            downloadUrl = info.downloadUrl,
                            releaseNotes = info.releaseNotes
                        )
                    } else if (info.latestVersion.isBlank()) {
                        _updateStatus.value = UpdateStatus.NoRelease
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                    }
                }
                .onFailure { error ->
                    _updateStatus.value = UpdateStatus.Error(error.localizedMessage ?: "Unknown error")
                    if (isManual) {
                        Toast.makeText(
                            getApplication(),
                            "Update check failed: ${error.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm:ss a")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        private val NEXT_ANNOUNCEMENT_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
    }
}
