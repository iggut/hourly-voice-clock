package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.di.DependenciesProvider
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
                val deps = (appContext as DependenciesProvider).dependencies
                val settings = deps.settingsRepository.settings.first()
                if (settings.hourlyAnnouncementsEnabled) {
                    deps.announcementScheduler.scheduleNextHour(settings.exactAlarmsEnabled)
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
