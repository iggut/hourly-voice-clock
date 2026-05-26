package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val settingsRepo = SettingsRepository(appContext)
                    val settings = settingsRepo.settings.first()
                    if (settings.hourlyAnnouncementsEnabled) {
                        val scheduler = AnnouncementScheduler(appContext)
                        scheduler.cancelHourlyAlarms()
                        scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
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
