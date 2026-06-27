package com.hourlyvoiceclock.ui.schedulesettings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.scheduler.DeviceGuidance
import com.hourlyvoiceclock.scheduler.ExactAlarmState
import com.hourlyvoiceclock.scheduler.ScheduleReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class ScheduleSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val deps = (application as DependenciesProvider).dependencies

    val quietHoursEnabled: StateFlow<Boolean> = deps.settingsRepository.settings
        .map { it.quietHoursEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val quietStart: StateFlow<LocalTime> = deps.settingsRepository.settings
        .map { it.quietHoursStart }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.of(22, 0))

    val quietEnd: StateFlow<LocalTime> = deps.settingsRepository.settings
        .map { it.quietHoursEnd }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.of(7, 0))

    val quietDaysQuietStart: StateFlow<LocalTime> = deps.settingsRepository.settings
        .map { it.quietDaysQuietStart }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.of(10, 0))

    val quietDaysQuietEnd: StateFlow<LocalTime> = deps.settingsRepository.settings
        .map { it.quietDaysQuietEnd }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.of(18, 0))

    val allowManualDuringQuiet: StateFlow<Boolean> = deps.settingsRepository.settings
        .map { it.allowManualDuringQuiet }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val quietDaysDisabled: StateFlow<Set<DayOfWeek>> = deps.settingsRepository.settings
        .map { it.quietDaysDisabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val exactAlarmsEnabled: StateFlow<Boolean> = deps.settingsRepository.settings
        .map { it.exactAlarmsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationLogging: StateFlow<Boolean> = deps.settingsRepository.settings
        .map { it.notificationLogging }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _exactAlarmState = MutableStateFlow(deps.exactAlarmCapability.current())
    val exactAlarmState: StateFlow<ExactAlarmState> = _exactAlarmState.asStateFlow()

    val canScheduleExact: Boolean
        get() = _exactAlarmState.value is ExactAlarmState.Granted

    val needsExactPermission: Boolean
        get() = exactAlarmsEnabled.value && _exactAlarmState.value is ExactAlarmState.Denied

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
        checkExactAlarmPermission()
        checkNotificationPermission()
        checkBatteryOptimizations()
    }

    fun checkExactAlarmPermission() {
        val previousState = _exactAlarmState.value
        val currentState = deps.exactAlarmCapability.current()
        _exactAlarmState.value = currentState

        val wasDenied = previousState is ExactAlarmState.Denied
        val isGranted = currentState is ExactAlarmState.Granted

        // If permission was previously denied and is now granted, reschedule
        if (wasDenied && isGranted && exactAlarmsEnabled.value) {
            viewModelScope.launch {
                val settings = deps.settingsRepository.settings.first()
                if (settings.hourlyAnnouncementsEnabled) {
                    deps.hourlySchedulePolicy.applyCurrentPolicy(ScheduleReason.EXACT_PERMISSION_CHANGED)
                }
            }
        }
    }

    fun deviceGuidance(): DeviceGuidance? {
        val state = _exactAlarmState.value
        return if (state is ExactAlarmState.Denied) state.guidance else null
    }

    fun openExactAlarmSettings() {
        val state = _exactAlarmState.value
        if (state is ExactAlarmState.Denied) {
            getApplication<Application>().startActivity(state.settingsIntent)
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
            deps.settingsRepository.update { it.copy(quietHoursEnabled = enabled) }
        }
    }

    fun setQuietStart(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(quietHoursStart = time) }
        }
    }

    fun setQuietEnd(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(quietHoursEnd = time) }
        }
    }

    fun setQuietDaysQuietStart(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(quietDaysQuietStart = time) }
        }
    }

    fun setQuietDaysQuietEnd(time: LocalTime) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(quietDaysQuietEnd = time) }
        }
    }

    fun setAllowManualDuringQuiet(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(allowManualDuringQuiet = enabled) }
        }
    }

    fun toggleQuietDay(day: DayOfWeek, disabled: Boolean) {
        viewModelScope.launch {
            val current = quietDaysDisabled.value.toMutableSet()
            if (disabled) current.add(day) else current.remove(day)
            deps.settingsRepository.update { it.copy(quietDaysDisabled = current) }
        }
    }

    fun setExactAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            deps.hourlySchedulePolicy.setExactRequested(enabled)
            _exactAlarmState.value = deps.exactAlarmCapability.current()
        }
    }

    fun setNotificationLogging(enabled: Boolean) {
        viewModelScope.launch {
            deps.settingsRepository.update { it.copy(notificationLogging = enabled) }
        }
    }
}
