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

    private val _seconds = MutableStateFlow("")
    val seconds: StateFlow<String> = _seconds.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _nextAnnouncement = MutableStateFlow("")
    val nextAnnouncement: StateFlow<String> = _nextAnnouncement.asStateFlow()

// ⚡ Bolt: Throttled state flow that only updates once per minute to reduce CPU/GC overhead in derived states.
    // ⚡ Bolt: Throttled now flow that only updates once per minute to prevent high-frequency combine evaluations
    private val _nowMinute = MutableStateFlow(clock.now())

    /** Derived directly from settings so it can never drift from the source of truth. */
    val hourlyEnabled: StateFlow<Boolean> = appSettings
        .map { it.hourlyAnnouncementsEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** Recomputed every minute from the current time and settings. */
    val quietHoursActive: StateFlow<Boolean> = combine(appSettings, _nowMinute) { settings, now ->
        computeQuietHoursActive(settings, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** Recomputed every minute from the current time and settings. */
    val canSpeakNow: StateFlow<Boolean> = combine(appSettings, _nowMinute) { settings, now ->
        computeCanSpeakNow(settings, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private var startupAutoCheckDone = false
    private var cachedNextAnnouncementHash: Long? = null

    private var cachedMinuteHash: Long? = null
    private var cachedYear = -1
    private var cachedDayOfYear = -1
    private var cachedEpochDay = 0L

    init {
        viewModelScope.launch {
            // ⚡ Bolt: Cache LocalDateTime.now() and reuse it if the current second hasn't changed.
            // This prevents allocating a new LocalDateTime object on every single 1-second tick
            // when the loop drifts or wakes up multiple times in the same second.
            var lastSecond = -1
            var cachedNow = clock.now()
            while (isActive) {
                val currentSecond = (clock.currentTimeMillis() / 1000).toInt()
                if (currentSecond != lastSecond) {
                    lastSecond = currentSecond
                    cachedNow = clock.now()
                }
                updateTime(cachedNow)
                if (hourlyEnabled.value) {
                    updateNextAnnouncement(true, cachedNow)
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

    // ⚡ Bolt: Cache epoch day calculation. Calculates epoch day only when the year or day of the year changes,
    // avoiding redundant `now.toLocalDate().toEpochDay()` allocations and math-heavy calculations on every single 1-second tick.
    private fun getEpochDay(now: LocalDateTime): Long {
        if (now.year != cachedYear || now.dayOfYear != cachedDayOfYear) {
            cachedYear = now.year
            cachedDayOfYear = now.dayOfYear
            cachedEpochDay = now.toLocalDate().toEpochDay()
        }
        return cachedEpochDay
    }

    private fun updateTime(now: LocalDateTime) {
        _seconds.value = clock.secondsText(now)
        val currentMinuteHash = getEpochDay(now) * 24 * 60 + now.hour * 60 + now.minute

        if (cachedMinuteHash != currentMinuteHash) {
            cachedMinuteHash = currentMinuteHash
    _nowMinute.value = now // ⚡ Bolt: Emit throttled time change
            _timeState.value = clock.timeState(now)
            _currentDate.value = clock.dateText(now)
        }
    }

    private fun updateNextAnnouncement(enabled: Boolean, now: LocalDateTime) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            cachedNextAnnouncementHash = null
            return
        }

        val currentHourHash = getEpochDay(now) * 24 + now.hour
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
