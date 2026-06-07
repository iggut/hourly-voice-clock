package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.AlarmManager
import com.hourlyvoiceclock.di.DependenciesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val deps = (appContext as DependenciesProvider).dependencies
                    val settings = deps.settingsRepository.settings.first()
                    if (settings.hourlyAnnouncementsEnabled) {
                        deps.announcementScheduler.cancelHourlyAlarms()
                        deps.announcementScheduler.scheduleNextHour(settings.exactAlarmsEnabled)
                        Log.d("TimeChangedReceiver", "Rescheduled after action: $action")
                    }
                } catch (e: Exception) {
                    Log.e("TimeChangedReceiver", "Error rescheduling after action $action", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
