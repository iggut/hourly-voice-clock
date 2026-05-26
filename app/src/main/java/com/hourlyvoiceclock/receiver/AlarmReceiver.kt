package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HOURLY_ALARM) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settingsRepo = SettingsRepository(appContext)
                val settings = settingsRepo.settings.first()

                if (!settings.hourlyAnnouncementsEnabled) {
                    Log.d("AlarmReceiver", "Hourly announcements disabled, skipping")
                    return@launch
                }

                // ALWAYS reschedule first, before any heavy TTS work.
                // If the process is killed during TTS init/speak, the next
                // alarm is already set.
                val scheduler = AnnouncementScheduler(appContext)
                scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
                Log.d("AlarmReceiver", "Rescheduled next hourly alarm")

                // Then do the announcement
                val app = appContext as? HourlyVoiceClockApp
                if (app == null) {
                    Log.e("AlarmReceiver", "Cannot cast context to HourlyVoiceClockApp")
                    return@launch
                }

                val ttsRepo = TtsVoiceRepository(app.ttsEngine)
                
                withContext(Dispatchers.Main) {
                    ttsRepo.initialize()
                    val announcer = TimeAnnouncer(appContext, ttsRepo)
                    // Hourly announcements always say the top of the hour,
                    // even if Doze delays the alarm by several minutes.
                    val scheduledHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
                    announcer.announce(settings, force = false, dateTime = scheduledHour)
                }

            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error handling alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HOURLY_ALARM = "com.hourlyvoiceclock.ACTION_HOURLY_ALARM"
    }
}
