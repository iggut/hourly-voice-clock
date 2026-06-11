package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.scheduler.ScheduleReason

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        launchAsync(context) { appContext ->
            val deps = (appContext as DependenciesProvider).dependencies
            deps.hourlySchedulePolicy.applyCurrentPolicy(ScheduleReason.BOOT)
            Log.d("BootReceiver", "Rescheduled hourly alarm after action: $action")
        }
    }
}
