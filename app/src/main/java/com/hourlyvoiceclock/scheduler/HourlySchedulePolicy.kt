package com.hourlyvoiceclock.scheduler

import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.HourlyScheduleSettingsStore
import kotlinx.coroutines.flow.first

enum class ScheduleReason(val cancelFirst: Boolean) {
    BOOT(cancelFirst = false),
    TIME_CHANGED(cancelFirst = true),
    EXACT_PERMISSION_CHANGED(cancelFirst = true),
    HOURLY_TOGGLED(cancelFirst = false)
}

data class ScheduleSyncResult(
    val settings: AppSettings,
    val canScheduleExactAlarms: Boolean,
    val scheduledExact: Boolean
) {
    val needsExactPermission: Boolean
        get() = !canScheduleExactAlarms
}

class HourlySchedulePolicy(
    private val settingsStore: HourlyScheduleSettingsStore,
    private val scheduler: HourlyAlarmScheduler,
    private val exactAlarmCapability: ExactAlarmCapability
) {

    suspend fun setEnabled(enabled: Boolean): ScheduleSyncResult {
        settingsStore.update { it.copy(hourlyAnnouncementsEnabled = enabled) }
        return applyCurrentPolicy(ScheduleReason.HOURLY_TOGGLED)
    }

    suspend fun applyCurrentPolicy(reason: ScheduleReason): ScheduleSyncResult {
        val settings = settingsStore.settings.first()
        val canExact = exactAlarmCapability.current() is ExactAlarmState.Granted
        return syncCurrentPolicy(settings, canExact, cancelFirst = reason.cancelFirst)
    }

    suspend fun onAlarmTriggered(): ScheduleSyncResult? {
        val settings = settingsStore.settings.first()
        if (!settings.hourlyAnnouncementsEnabled) return null

        val canExact = exactAlarmCapability.current() is ExactAlarmState.Granted
        return syncCurrentPolicy(settings, canExact, cancelFirst = false)
    }

    private suspend fun syncCurrentPolicy(
        settings: AppSettings,
        canExact: Boolean,
        cancelFirst: Boolean
    ): ScheduleSyncResult {
        if (!settings.hourlyAnnouncementsEnabled) {
            scheduler.cancelHourlyAlarms()
            return ScheduleSyncResult(
                settings = settings,
                canScheduleExactAlarms = canExact,
                scheduledExact = false
            )
        }

        if (cancelFirst) {
            scheduler.cancelHourlyAlarms()
        }

        val scheduledExact = canExact
        scheduler.scheduleNextHour(scheduledExact)

        return ScheduleSyncResult(
            settings = settings,
            canScheduleExactAlarms = canExact,
            scheduledExact = scheduledExact
        )
    }
}
