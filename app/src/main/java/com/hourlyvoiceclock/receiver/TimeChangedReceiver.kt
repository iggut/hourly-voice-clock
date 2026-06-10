package com.hourlyvoiceclock.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.scheduler.rescheduleAnnouncements

class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            launchAsync(context) { appContext ->
                rescheduleAnnouncements(appContext, cancelFirst = true)
                Log.d("TimeChangedReceiver", "Rescheduled after action: $action")
            }
        }
    }
}
