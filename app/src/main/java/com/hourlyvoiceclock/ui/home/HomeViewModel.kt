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

data class TimeDisplayState(
    val hoursMinutes: String = "",
    val seconds: String = "",
    val amPm: String = ""
)

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

    private val _timeState = MutableStateFlow(TimeDisplayState())
    val timeState: StateFlow<TimeDisplayState> = _timeState.asStateFlow()

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
    private var cachedMinuteHash: Int? = null
    private var cachedHoursMinutes: String = ""
    private var cachedAmPm: String = ""
    private var cachedDayHash: Int? = null
    private var cachedNextAnnouncementHash: Int? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                updateTime(now)
                if (_hourlyEnabled.value) {
                    updateNextAnnouncement(true, now)
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
            val selectedPackage = deps.ttsEngineSelector.select()
            val initOk = deps.ttsEngine.initialize(selectedPackage)
            if (!initOk) {
                Toast.makeText(getApplication(), "Text-to-Speech initialization failed. Check your TTS engine in system settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTime(now: LocalDateTime) {
        // Create an integer hash representing the minute: e.g. Day 100 * 24 * 60 + hour * 60 + minute
        // This prevents the clock from not updating if the app is suspended for exactly 1 hour.
        val currentMinuteHash = now.dayOfYear * 1440 + now.hour * 60 + now.minute

        // Optimize: Only format hours/minutes and am/pm when the minute changes
        if (currentMinuteHash != cachedMinuteHash) {
            cachedMinuteHash = currentMinuteHash
            cachedHoursMinutes = now.format(HOURS_MINUTES_FORMATTER)
            cachedAmPm = now.format(AM_PM_FORMATTER)
        }

        // Fast path for seconds to avoid formatter overhead
        val secondsStr = SECONDS_CACHE[now.second]

        _timeState.value = TimeDisplayState(
            hoursMinutes = cachedHoursMinutes,
            seconds = secondsStr,
            amPm = cachedAmPm
        )

        // Optimize: Only format date when the day changes
        val currentDayHash = now.year * 400 + now.dayOfYear
        if (cachedDayHash != currentDayHash) {
            cachedDayHash = currentDayHash
            _currentDate.value = now.format(DATE_FORMATTER)
        }
    }

    private fun updateNextAnnouncement(enabled: Boolean, now: LocalDateTime) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            cachedNextAnnouncementHash = null
            return
        }

        val currentHourHash = now.dayOfYear * 24 + now.hour
        // Optimize: Only format next announcement when the target hour changes
        if (cachedNextAnnouncementHash != currentHourHash) {
            cachedNextAnnouncementHash = currentHourHash
            val next = AnnouncementScheduler.getNextTopOfHour(now)
            _nextAnnouncement.value = next.format(NEXT_ANNOUNCEMENT_FORMATTER)
        }
    }

    private fun updateQuietStatus(settings: com.hourlyvoiceclock.data.AppSettings) {
        val now = java.time.LocalTime.now()
        val today = java.time.LocalDate.now().dayOfWeek
        val inQuiet = QuietHoursPolicy.isQuietTime(
            now,
            settings.quietHoursEnabled,
            settings.quietHoursStart,
            settings.quietHoursEnd,
            settings.quietDaysDisabled,
            today,
            settings.quietDaysQuietStart,
            settings.quietDaysQuietEnd
        )
        _quietHoursActive.value = inQuiet
        _canSpeakNow.value = QuietHoursPolicy.canAnnounceManually(
            now,
            settings.quietHoursEnabled,
            settings.quietHoursStart,
            settings.quietHoursEnd,
            settings.allowManualDuringQuiet,
            settings.quietDaysDisabled,
            today,
            settings.quietDaysQuietStart,
            settings.quietDaysQuietEnd
        )
    }

    fun toggleHourly(enabled: Boolean) {
        viewModelScope.launch {
            deps.hourlySchedulePolicy.setEnabled(enabled)
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
        private val HOURS_MINUTES_FORMATTER = DateTimeFormatter.ofPattern("h:mm")
        private val AM_PM_FORMATTER = DateTimeFormatter.ofPattern("a")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        private val NEXT_ANNOUNCEMENT_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

        // Precompute seconds strings to avoid GC pressure from dynamic allocation in 1-second ticks
        private val SECONDS_CACHE = Array(60) { it.toString().padStart(2, '0') }
    }
}
