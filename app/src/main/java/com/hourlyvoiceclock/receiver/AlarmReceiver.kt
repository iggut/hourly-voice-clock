package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.tts.AndroidTtsEngine
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HOURLY_ALARM) return

        val appContext = context.applicationContext
        val settingsRepo = SettingsRepository(appContext)
        val scheduler = AnnouncementScheduler(appContext)

        CoroutineScope(Dispatchers.Main).launch {
            val settings = settingsRepo.settings.first()
            if (!settings.hourlyAnnouncementsEnabled) return@launch

            val ttsEngine = AndroidTtsEngine(appContext)
            val ttsRepo = TtsVoiceRepository(ttsEngine)
            val announcer = TimeAnnouncer(appContext, ttsRepo)

            try {
                announcer.announce(settings, force = false)
            } finally {
                ttsEngine.shutdown()
            }

            if (settings.hourlyAnnouncementsEnabled) {
                scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
            }
        }
    }

    companion object {
        const val ACTION_HOURLY_ALARM = "com.hourlyvoiceclock.ACTION_HOURLY_ALARM"
    }
}
