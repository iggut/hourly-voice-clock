package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.scheduler.rescheduleAnnouncements

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        launchAsync(context) { appContext ->
            rescheduleAnnouncements(appContext)
            Log.d("BootReceiver", "Rescheduled hourly alarm after action: $action")
        }
    }
}
