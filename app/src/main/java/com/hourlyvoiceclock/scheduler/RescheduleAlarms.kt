package com.hourlyvoiceclock.scheduler

import android.content.Context
import com.hourlyvoiceclock.di.DependenciesProvider
import kotlinx.coroutines.flow.first

/**
 * Shared scheduling logic for BroadcastReceivers.
 * Reads settings from the repository and schedules (or reschedules)
 * the hourly alarm if announcements are enabled.
 *
 * @param appContext Application context (must implement DependenciesProvider)
 * @param cancelFirst If true, cancels existing alarms before scheduling
 *   (used by TimeChangedReceiver for timezone/time changes)
 */
internal suspend fun rescheduleAnnouncements(
    appContext: Context,
    cancelFirst: Boolean = false
) {
    val deps = (appContext as DependenciesProvider).dependencies
    val settings = deps.settingsRepository.settings.first()
    if (settings.hourlyAnnouncementsEnabled) {
        if (cancelFirst) {
            deps.announcementScheduler.cancelHourlyAlarms()
        }
        deps.announcementScheduler.scheduleNextHour(settings.exactAlarmsEnabled)
    }
}
