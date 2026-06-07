package com.hourlyvoiceclock.ui.home

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.announcer.QuietHoursPolicy
import com.hourlyvoiceclock.data.UpdateStatus
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val deps = (application as DependenciesProvider).dependencies
    private val updateManager = deps.updateManager

    val appSettings: StateFlow<com.hourlyvoiceclock.data.AppSettings> = deps.settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.hourlyvoiceclock.data.AppSettings()
        )

    val updateStatus: StateFlow<UpdateStatus> = updateManager.status

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

    private var startupAutoCheckDone = false
    private var cachedDate: java.time.LocalDate? = null
    private var cachedNextAnnouncementTime: LocalDateTime? = null

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
            var previousAutoUpdateEnabled: Boolean? = null
            deps.settingsRepository.settings.collect { settings ->
                _hourlyEnabled.value = settings.hourlyAnnouncementsEnabled
                updateQuietStatus(settings)
                val reEnabled =
                    previousAutoUpdateEnabled == false && settings.autoUpdateEnabled
                previousAutoUpdateEnabled = settings.autoUpdateEnabled
                if (settings.autoUpdateEnabled && (!startupAutoCheckDone || reEnabled)) {
                    startupAutoCheckDone = true
                    checkForUpdates(isManual = false)
                }
            }
        }
        viewModelScope.launch {
            val initOk = deps.ttsEngine.initialize()
            if (!initOk) {
                Toast.makeText(getApplication(), "Text-to-Speech initialization failed. Check your TTS engine in system settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTime() {
        val now = LocalDateTime.now()
        // Format time every second
        _currentTime.value = now.format(TIME_FORMATTER)

        // Optimize: Only format date when the day changes
        val today = now.toLocalDate()
        if (cachedDate != today) {
            cachedDate = today
            _currentDate.value = now.format(DATE_FORMATTER)
        }
    }

    private fun updateNextAnnouncement(enabled: Boolean) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            cachedNextAnnouncementTime = null
            return
        }
        val next = AnnouncementScheduler.getNextTopOfHour()
        // Optimize: Only format next announcement when the target hour changes
        if (cachedNextAnnouncementTime != next) {
            cachedNextAnnouncementTime = next
            _nextAnnouncement.value = next.format(NEXT_ANNOUNCEMENT_FORMATTER)
        }
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
            deps.settingsRepository.update { it.copy(hourlyAnnouncementsEnabled = enabled) }
            val settings = deps.settingsRepository.settings.first()
            if (enabled) {
                deps.announcementScheduler.scheduleNextHour(settings.exactAlarmsEnabled)
            } else {
                deps.announcementScheduler.cancelHourlyAlarms()
            }
        }
    }

    fun announceNow(includeDate: Boolean = false) {
        viewModelScope.launch {
            val settings = deps.settingsRepository.settings.first()
            deps.timeAnnouncer.announce(settings, force = true, includeDate = includeDate)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.setAutoUpdateEnabled(enabled)
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        val currentVersion = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(
                getApplication<Application>().packageName, 0
            )
            pInfo.versionName ?: "0.1"
        } catch (_: Exception) {
            "0.1"
        }
        updateManager.checkForUpdate(currentVersion, isManual)
    }

    fun downloadAndInstall(downloadUrl: String) {
        val context = getApplication<Application>()
        updateManager.downloadAndInstall(downloadUrl, context.cacheDir)
    }

    fun cancelDownload() = updateManager.cancelDownload()

    fun installApk(localPath: String) {
        updateManager.installApk(getApplication(), localPath)
    }

    fun cleanupAfterInstall(localPath: String?) {
        updateManager.cleanupAfterInstall(localPath)
    }

    fun dismissUpdateDialog() = updateManager.dismissUpdateDialog()

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm:ss a")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        private val NEXT_ANNOUNCEMENT_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
    }
}
