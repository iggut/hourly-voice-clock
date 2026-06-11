package com.hourlyvoiceclock.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hourlyvoiceclock.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AnnouncementScheduler(private val context: Context) : HourlyAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleNextHour(exact: Boolean) {
        val nextHour = getNextTopOfHour()
        val triggerAtMillis = nextHour.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HOURLY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_HOURLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (exact && AlarmPermissionChecker.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d("AnnouncementScheduler", "Exact alarm scheduled for $nextHour")
                return
            }
        } catch (e: SecurityException) {
            Log.e("AnnouncementScheduler", "SecurityException scheduling alarm", e)
        }

        // Fallback or inexact schedule
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        Log.d("AnnouncementScheduler", "Inexact alarm scheduled for $nextHour")
    }

    override fun cancelHourlyAlarms() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HOURLY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_HOURLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d("AnnouncementScheduler", "Hourly alarms cancelled")
    }

    companion object {
        private const val REQUEST_CODE_HOURLY = 1001

        fun getNextTopOfHour(from: LocalDateTime = LocalDateTime.now()): LocalDateTime {
            return from.plusHours(1).truncatedTo(ChronoUnit.HOURS)
        }
    }
}
