package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.di.DependenciesProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HOURLY_ALARM) return

        launchAsync(context) { appContext ->
            val deps = (appContext as DependenciesProvider).dependencies
            val result = deps.hourlySchedulePolicy.onAlarmTriggered()

            if (result == null) {
                Log.d("AlarmReceiver", "Hourly announcements disabled, skipping")
                return@launchAsync
            }

            Log.d("AlarmReceiver", "Rescheduled next hourly alarm")

            // Then do the announcement
            withContext(Dispatchers.Main) {
                val selectedPackage = deps.ttsEngineSelector.select()
                deps.ttsEngine.initialize(selectedPackage)
                val scheduledHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
                deps.timeAnnouncer.announce(result.settings, force = false, dateTime = scheduledHour)
            }
        }
    }

    companion object {
        const val ACTION_HOURLY_ALARM = "com.hourlyvoiceclock.ACTION_HOURLY_ALARM"
    }
}
