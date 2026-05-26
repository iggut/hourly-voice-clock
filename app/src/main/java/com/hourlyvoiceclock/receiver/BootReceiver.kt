package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settingsRepo = SettingsRepository(appContext)
                val settings = settingsRepo.settings.first()
                if (settings.hourlyAnnouncementsEnabled) {
                    val scheduler = AnnouncementScheduler(appContext)
                    scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
                    Log.d("BootReceiver", "Rescheduled hourly alarm after action: $action")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling after action $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
