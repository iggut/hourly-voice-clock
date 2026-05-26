package com.hourlyvoiceclock.ui.schedulesettings

import android.app.Application
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AlarmPermissionChecker
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

class ScheduleSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val scheduler = AnnouncementScheduler(application)

    private val _quietHoursEnabled = MutableStateFlow(false)
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()

    private val _quietStart = MutableStateFlow(LocalTime.of(22, 0))
    val quietStart: StateFlow<LocalTime> = _quietStart.asStateFlow()

    private val _quietEnd = MutableStateFlow(LocalTime.of(7, 0))
    val quietEnd: StateFlow<LocalTime> = _quietEnd.asStateFlow()

    private val _allowManualDuringQuiet = MutableStateFlow(true)
    val allowManualDuringQuiet: StateFlow<Boolean> = _allowManualDuringQuiet.asStateFlow()

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

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(true)
    val isIgnoringBatteryOptimizations: StateFlow<Boolean> = _isIgnoringBatteryOptimizations.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            _quietHoursEnabled.value = settings.quietHoursEnabled
            _quietStart.value = settings.quietHoursStart
            _quietEnd.value = settings.quietHoursEnd
            _allowManualDuringQuiet.value = settings.allowManualDuringQuiet
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
                val settings = settingsRepo.settings.first()
                if (settings.hourlyAnnouncementsEnabled) {
                    scheduler.cancelHourlyAlarms()
                    scheduler.scheduleNextHour(exact = true)
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
            _isIgnoringBatteryOptimizations.value = pm.isIgnoringBatteryOptimizations(app.packageName)
        } else {
            _isIgnoringBatteryOptimizations.value = true
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setQuietHoursEnabled(enabled)
            _quietHoursEnabled.value = enabled
        }
    }

    fun setQuietStart(time: LocalTime) {
        viewModelScope.launch {
            settingsRepo.setQuietHoursStart(time)
            _quietStart.value = time
        }
    }

    fun setQuietEnd(time: LocalTime) {
        viewModelScope.launch {
            settingsRepo.setQuietHoursEnd(time)
            _quietEnd.value = time
        }
    }

    fun setAllowManualDuringQuiet(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAllowManualDuringQuiet(enabled)
            _allowManualDuringQuiet.value = enabled
        }
    }

    fun setExactAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setExactAlarmsEnabled(enabled)
            _exactAlarmsEnabled.value = enabled

            if (enabled) {
                // Re-check permission immediately
                checkExactAlarmPermission()
                // If still can't schedule exact after re-check, we'll show the permission card
                if (_needsExactPermission.value) {
                    // Permission not granted yet — don't reschedule with exact timing
                    return@launch
                }
            }

            // Schedule or reschedule
            val settings = settingsRepo.settings.first()
            if (settings.hourlyAnnouncementsEnabled) {
                scheduler.cancelHourlyAlarms()
                scheduler.scheduleNextHour(enabled && _canScheduleExact.value)
            }
        }
    }

    fun setNotificationLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setNotificationLogging(enabled)
            _notificationLogging.value = enabled
        }
    }
}
