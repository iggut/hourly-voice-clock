package com.hourlyvoiceclock.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AnnouncementSchedulerTest {

    @Test
    fun `scheduleNextHour exact alarm throws SecurityException falls back to inexact`() {
        val context = mock(Context::class.java)
        val alarmManager = mock(AlarmManager::class.java)

        `when`(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        // Ensure canScheduleExactAlarms returns true to trigger the try block
        `when`(alarmManager.canScheduleExactAlarms()).thenReturn(true)
        // Setup to throw SecurityException
        doThrow(SecurityException::class.java).`when`(alarmManager)
            .setExactAndAllowWhileIdle(anyInt(), anyLong(), any(PendingIntent::class.java))

        val scheduler = AnnouncementScheduler(context)
        scheduler.scheduleNextHour(exact = true)

        // Verify fallback is called
        verify(alarmManager).setAndAllowWhileIdle(anyInt(), anyLong(), any(PendingIntent::class.java))
    }
}
