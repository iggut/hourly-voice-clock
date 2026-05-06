package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val appContext = context.applicationContext
                val settingsRepo = SettingsRepository(appContext)
                val scheduler = AnnouncementScheduler(appContext)

                CoroutineScope(Dispatchers.Main).launch {
                    val settings = settingsRepo.settings.first()
                    if (settings.hourlyAnnouncementsEnabled) {
                        scheduler.cancelHourlyAlarms()
                        scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
                    }
                }
            }
        }
    }
}
