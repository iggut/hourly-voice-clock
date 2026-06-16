package com.hourlyvoiceclock.ui.schedulesettings

import android.app.Application
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.scheduler.AlarmPermissionChecker
import com.hourlyvoiceclock.scheduler.ScheduleReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class ScheduleSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val deps = (application as DependenciesProvider).dependencies

    private val _quietHoursEnabled = MutableStateFlow(false)
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()

    private val _quietStart = MutableStateFlow(LocalTime.of(22, 0))
    val quietStart: StateFlow<LocalTime> = _quietStart.asStateFlow()

    private val _quietEnd = MutableStateFlow(LocalTime.of(7, 0))
    val quietEnd: StateFlow<LocalTime> = _quietEnd.asStateFlow()

    private val _quietDaysQuietStart = MutableStateFlow(LocalTime.of(10, 0))
    val quietDaysQuietStart: StateFlow<LocalTime> = _quietDaysQuietStart.asStateFlow()

    private val _quietDaysQuietEnd = MutableStateFlow(LocalTime.of(18, 0))
    val quietDaysQuietEnd: StateFlow<LocalTime> = _quietDaysQuietEnd.asStateFlow()

    private val _allowManualDuringQuiet = MutableStateFlow(true)
    val allowManualDuringQuiet: StateFlow<Boolean> = _allowManualDuringQuiet.asStateFlow()

    private val _quietDaysDisabled = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    val quietDaysDisabled: StateFlow<Set<DayOfWeek>> = _quietDaysDisabled.asStateFlow()

    private val _exactAlarmsEnabled = MutableStateFlow(false)
    val exactAlarmsEnabled: StateFlow<Boolean> = _exactAlarmsEnabled.asStateFlow()

    // Whether the system currently allows exact alarms (re-checked on resume)
    private val _canScheduleExact = MutableStateFlow(false)
    val canScheduleExact: StateFlow<Boolean> = _canScheduleExact.asStateFlow()

    // True when user has enabled exact alarms but permission is not granted
    private val _needsExactPermission = MutableStateFlow(false)
    val needsExactPermission: StateFlow<Boolean> = _needsExactPermission.asStateFlow()

    private val _notificationLogging = MutableStateFlow(false)
    val notificationLogging: StateFlow<Boolean> = _notificationLogging.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    enum class BatteryOptimizationStatus {
        UNKNOWN,
        OPTIMIZED,
        UNRESTRICTED
    }

    private val _batteryStatus = MutableStateFlow(BatteryOptimizationStatus.UNKNOWN)
    val batteryStatus: StateFlow<BatteryOptimizationStatus> = _batteryStatus.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            val settings = deps.settingsRepository.settings.first()
            _quietHoursEnabled.value = settings.quietHoursEnabled
            _quietStart.value = settings.quietHoursStart
            _quietEnd.value = settings.quietHoursEnd
            _quietDaysQuietStart.value = settings.quietDaysQuietStart
            _quietDaysQuietEnd.value = settings.quietDaysQuietEnd
            _allowManualDuringQuiet.value = settings.allowManualDuringQuiet
            _quietDaysDisabled.value = settings.quietDaysDisabled
            _exactAlarmsEnabled.value = settings.exactAlarmsEnabled
            _notificationLogging.value = settings.notificationLogging
            checkExactAlarmPermission()
            checkNotificationPermission()
            checkBatteryOptimizations()
        }
    }

    fun checkExactAlarmPermission() {
        val can = AlarmPermissionChecker.canScheduleExactAlarms(getApplication())
        val wasDenied = _needsExactPermission.value
        _canScheduleExact.value = can

        // If exact alarms are requested but permission not granted, show the permission card
        _needsExactPermission.value = _exactAlarmsEnabled.value && !can &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        // If permission was previously denied and is now granted, reschedule
        if (wasDenied && can && _exactAlarmsEnabled.value) {
            viewModelScope.launch {
                val settings = deps.settingsRepository.settings.first()
                if (settings.hourlyAnnouncementsEnabled) {
                    deps.hourlySchedulePolicy.applyCurrentPolicy(ScheduleReason.EXACT_PERMISSION_CHANGED)
                }
            }
        }
    }

    fun onResume() {
        checkExactAlarmPermission()
        checkNotificationPermission()
        checkBatteryOptimizations()
    }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            _hasNotificationPermission.value = granted
        } else {
            _hasNotificationPermission.value = true
        }
    }

    fun checkBatteryOptimizations() {
        val app = getApplication<Application>()
        val pm = app.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        if (pm != null) {
            val ignoring = pm.isIgnoringBatteryOptimizations(app.packageName)
            _batteryStatus.value = if (ignoring) BatteryOptimizationStatus.UNRESTRICTED else BatteryOptimizationStatus.OPTIMIZED
        } else {
            _batteryStatus.value = BatteryOptimizationStatus.UNRESTRICTED
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.setQuietHoursEnabled(enabled)
            _quietHoursEnabled.value = enabled
        }
    }

    fun setQuietStart(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.setQuietHoursStart(time)
            _quietStart.value = time
        }
    }

    fun setQuietEnd(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.setQuietHoursEnd(time)
            _quietEnd.value = time
        }
    }

    fun setQuietDaysQuietStart(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.setQuietDaysQuietStart(time)
            _quietDaysQuietStart.value = time
        }
    }

    fun setQuietDaysQuietEnd(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.setQuietDaysQuietEnd(time)
            _quietDaysQuietEnd.value = time
        }
    }

    fun setAllowManualDuringQuiet(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.setAllowManualDuringQuiet(enabled)
            _allowManualDuringQuiet.value = enabled
        }
    }

    fun toggleQuietDay(day: DayOfWeek, disabled: Boolean) {
        viewModelScope.launch {
            val current = _quietDaysDisabled.value.toMutableSet()
            if (disabled) current.add(day) else current.remove(day)
            deps.settingsRepository.setQuietDaysDisabled(current)
            _quietDaysDisabled.value = current
        }
    }

    fun setExactAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val result = deps.hourlySchedulePolicy.setExactRequested(enabled)
            _exactAlarmsEnabled.value = enabled
            _canScheduleExact.value = result.canScheduleExactAlarms
            _needsExactPermission.value = result.needsExactPermission
        }
    }

    fun setNotificationLogging(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.setNotificationLogging(enabled)
            _notificationLogging.value = enabled
        }
    }
}
