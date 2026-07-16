package com.hourlyvoiceclock.ui.home

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.announcer.QuietHoursPolicy
import com.hourlyvoiceclock.data.PackageInfoProvider
import com.hourlyvoiceclock.data.UpdateStatus
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class TimeDisplayState(
    val hoursMinutes: String = "",
    val seconds: String = "",
    val amPm: String = ""
)

class HomeViewModel(
    application: Application,
    private val clock: Clock = Clock(),
    private val packageInfoProvider: PackageInfoProvider
) : AndroidViewModel(application) {

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

    private val _now = MutableStateFlow(clock.now())

    /** Derived directly from settings so it can never drift from the source of truth. */
    val hourlyEnabled: StateFlow<Boolean> = appSettings
        .map { it.hourlyAnnouncementsEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** Recomputed every second from the current time and settings. */
    val quietHoursActive: StateFlow<Boolean> = combine(appSettings, _now) { settings, now ->
        computeQuietHoursActive(settings, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** Recomputed every second from the current time and settings. */
    val canSpeakNow: StateFlow<Boolean> = combine(appSettings, _now) { settings, now ->
        computeCanSpeakNow(settings, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private var startupAutoCheckDone = false
    private var cachedNextAnnouncementHash: Int? = null

    // ⚡ Bolt: Cache formatted time strings to avoid allocations on every 1s tick
    private var cachedDateHash: Int? = null
    private var cachedMinuteHash: Int? = null
    private var cachedHoursMinutes = ""
    private var cachedAmPm = ""

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = clock.now()
                _now.value = now
                updateTime(now)
                if (hourlyEnabled.value) {
                    updateNextAnnouncement(true, now)
                }
                delay(1000)
            }
        }
        viewModelScope.launch {
            var previousAutoUpdateEnabled: Boolean? = null
            appSettings.collect { settings ->
                val reEnabled = previousAutoUpdateEnabled == false && settings.autoUpdateEnabled
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
        // ⚡ Bolt: Only reformat strings when the underlying minute or day changes
        val dateHash = now.year * 400 + now.dayOfYear
        if (cachedDateHash != dateHash) {
            cachedDateHash = dateHash
            _currentDate.value = clock.dateText(now)
        }

        val minuteHash = dateHash * 1440 + now.hour * 60 + now.minute
        if (cachedMinuteHash != minuteHash) {
            cachedMinuteHash = minuteHash
            cachedHoursMinutes = clock.formatHoursMinutes(now)
            cachedAmPm = clock.formatAmPm(now)
        }

        _timeState.value = TimeDisplayState(
            hoursMinutes = cachedHoursMinutes,
            seconds = clock.formatSeconds(now.second),
            amPm = cachedAmPm
        )
    }

    private fun updateNextAnnouncement(enabled: Boolean, now: LocalDateTime) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            cachedNextAnnouncementHash = null
            return
        }

        // ⚡ Bolt: Include year in hash to prevent leap-year collisions across year boundaries
        val currentHourHash = (now.year * 400 + now.dayOfYear) * 24 + now.hour
        // Optimize: Only format next announcement when the target hour changes
        if (cachedNextAnnouncementHash != currentHourHash) {
            cachedNextAnnouncementHash = currentHourHash
            _nextAnnouncement.value = clock.nextAnnouncementText(now, enabled = true)
        }
    }

    fun toggleHourly(enabled: Boolean) {
        viewModelScope.launch {
            deps.hourlySchedulePolicy.setEnabled(enabled)
        }
    }

    fun announceNow(includeDate: Boolean = false) {
        viewModelScope.launch {
            val settings = appSettings.first()
            deps.timeAnnouncer.announce(settings, force = true, includeDate = includeDate)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(autoUpdateEnabled = enabled) }
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        val currentVersion = packageInfoProvider.versionName()
        updateManager.checkForUpdate(currentVersion, isManual)
    }

    fun downloadAndInstall(downloadUrl: String) {
        val context = getApplication<Application>()
        updateManager.downloadAndInstall(downloadUrl, context.cacheDir)
    }

    fun cancelDownload() = updateManager.cancelDownload()

    fun installApk(localPath: String) {
        updateManager.installApk(localPath)
    }

    fun cleanupAfterInstall(localPath: String?) {
        updateManager.cleanupAfterInstall(localPath)
    }

    fun dismissUpdateDialog() = updateManager.dismissUpdateDialog()

    companion object {

        private fun computeQuietHoursActive(
            settings: com.hourlyvoiceclock.data.AppSettings,
            now: LocalDateTime
        ): Boolean {
            return QuietHoursPolicy.isQuietTime(
                now = now.toLocalTime(),
                quietHoursEnabled = settings.quietHoursEnabled,
                quietStart = settings.quietHoursStart,
                quietEnd = settings.quietHoursEnd,
                quietDaysDisabled = settings.quietDaysDisabled,
                currentDay = now.dayOfWeek,
                quietDaysStart = settings.quietDaysQuietStart,
                quietDaysEnd = settings.quietDaysQuietEnd
            )
        }

        private fun computeCanSpeakNow(
            settings: com.hourlyvoiceclock.data.AppSettings,
            now: LocalDateTime
        ): Boolean {
            return QuietHoursPolicy.canAnnounceManually(
                now = now.toLocalTime(),
                quietHoursEnabled = settings.quietHoursEnabled,
                quietStart = settings.quietHoursStart,
                quietEnd = settings.quietHoursEnd,
                allowManualDuringQuiet = settings.allowManualDuringQuiet,
                quietDaysDisabled = settings.quietDaysDisabled,
                currentDay = now.dayOfWeek,
                quietDaysStart = settings.quietDaysQuietStart,
                quietDaysEnd = settings.quietDaysQuietEnd
            )
        }
    }
}
