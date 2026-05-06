package com.hourlyvoiceclock.ui.schedulesettings

import android.app.Application
import android.provider.Settings
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

    private val _canScheduleExact = MutableStateFlow(false)
    val canScheduleExact: StateFlow<Boolean> = _canScheduleExact.asStateFlow()

    private val _notificationLogging = MutableStateFlow(false)
    val notificationLogging: StateFlow<Boolean> = _notificationLogging.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            _quietHoursEnabled.value = settings.quietHoursEnabled
            _quietStart.value = settings.quietHoursStart
            _quietEnd.value = settings.quietHoursEnd
            _allowManualDuringQuiet.value = settings.allowManualDuringQuiet
            _exactAlarmsEnabled.value = settings.exactAlarmsEnabled
            _notificationLogging.value = settings.notificationLogging
            _canScheduleExact.value = AlarmPermissionChecker.canScheduleExactAlarms(getApplication())
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
            val settings = settingsRepo.settings.first()
            if (settings.hourlyAnnouncementsEnabled) {
                scheduler.cancelHourlyAlarms()
                scheduler.scheduleNextHour(enabled)
            }
        }
    }

    fun setNotificationLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setNotificationLogging(enabled)
            _notificationLogging.value = enabled
        }
    }

    fun checkExactAlarmPermission() {
        _canScheduleExact.value = AlarmPermissionChecker.canScheduleExactAlarms(getApplication())
    }
}
