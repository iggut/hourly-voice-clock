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
        when (intent?.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
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
                            scheduler.cancelHourlyAlarms()
                            scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
                            Log.d("TimeChangedReceiver", "Rescheduled after time/timezone change")
                        }
                    } catch (e: Exception) {
                        Log.e("TimeChangedReceiver", "Error rescheduling", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
