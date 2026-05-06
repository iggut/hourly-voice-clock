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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val settingsRepo = SettingsRepository(appContext)
        val scheduler = AnnouncementScheduler(appContext)

        CoroutineScope(Dispatchers.Main).launch {
            val settings = settingsRepo.settings.first()
            if (settings.hourlyAnnouncementsEnabled) {
                scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
            }
        }
    }
}
