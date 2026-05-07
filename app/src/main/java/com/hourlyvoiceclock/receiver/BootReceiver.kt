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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val settingsRepo = SettingsRepository(appContext)
                val settings = settingsRepo.settings.first()
                if (settings.hourlyAnnouncementsEnabled) {
                    val app = appContext as? HourlyVoiceClockApp
                    app?.ttsEngine?.initialize()
                    val scheduler = AnnouncementScheduler(appContext)
                    scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
                    Log.d("BootReceiver", "Rescheduled hourly alarm after boot")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
