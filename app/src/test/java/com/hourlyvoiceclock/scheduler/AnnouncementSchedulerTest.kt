package com.hourlyvoiceclock.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.hourlyvoiceclock.receiver.AlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class AnnouncementSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: AnnouncementScheduler
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        scheduler = AnnouncementScheduler(context)
    }

    @Test
    fun testCancelHourlyAlarms_cancelsAlarmAndPendingIntent() {
        // Arrange
        // Schedule an alarm first so we have something to cancel
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HOURLY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001, // REQUEST_CODE_HOURLY
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 10000,
            pendingIntent
        )

        // Verify it was scheduled
        val scheduledAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("Alarm should be scheduled initially", scheduledAlarm)
        assertEquals(pendingIntent, scheduledAlarm?.operation)

        // Act
        scheduler.cancelHourlyAlarms()

        // Assert
        // Verify alarm is cancelled
        assertNull("Alarm should be cancelled", shadowAlarmManager.nextScheduledAlarm)

        // Verify PendingIntent is cancelled by trying to recreate it with FLAG_NO_CREATE
        val cancelledPendingIntent = PendingIntent.getBroadcast(
            context,
            1001, // REQUEST_CODE_HOURLY
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNull("PendingIntent should be cancelled", cancelledPendingIntent)
    }
}
