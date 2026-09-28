package com.hourlyvoiceclock.scheduler

import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.HourlyScheduleSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlySchedulePolicyTest {

    private fun fakeCapability(granted: Boolean): ExactAlarmCapability =
        object : ExactAlarmCapability {
            override fun current(): ExactAlarmState =
                if (granted) ExactAlarmState.Granted else ExactAlarmState.Denied(
                    canRequest = true,
                    settingsIntent = android.content.Intent(),
                    guidance = DeviceGuidance("Test", "Test", null)
                )
        }

    @Test
    fun `enable hourly announcements schedules an exact alarm`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = false, exactAlarmsEnabled = false)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler, fakeCapability(granted = true))

        val result = policy.setEnabled(true)

        assertTrue(result.scheduledExact)
        assertEquals(listOf("schedule:exact"), scheduler.calls)
        assertTrue(store.settings.first().hourlyAnnouncementsEnabled)
    }

    @Test
    fun `missing exact permission is reported and the alarm stays inexact`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = true)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler, fakeCapability(granted = false))

        val result = policy.applyCurrentPolicy(ScheduleReason.HOURLY_TOGGLED)

        assertTrue(result.needsExactPermission)
        assertFalse(result.scheduledExact)
        assertEquals(listOf("schedule:inexact"), scheduler.calls)
    }

    @Test
    fun `boot reconciliation schedules from current policy`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = true)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler, fakeCapability(granted = true))

        val result = policy.applyCurrentPolicy(ScheduleReason.BOOT)

        assertTrue(result.scheduledExact)
        assertEquals(listOf("schedule:exact"), scheduler.calls)
    }

    @Test
    fun `time change reconciliation cancels first and then reschedules an exact alarm`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = false)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler, fakeCapability(granted = true))

        val result = policy.applyCurrentPolicy(ScheduleReason.TIME_CHANGED)

        assertTrue(result.scheduledExact)
        assertEquals(listOf("cancel", "schedule:exact"), scheduler.calls)
    }

    @Test
    fun `alarm trigger reschedules before returning settings`() = runBlocking {
        val settings = AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = false)
        val store = FakeHourlyScheduleSettingsStore(settings)
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler, fakeCapability(granted = true))

        val result = policy.onAlarmTriggered()

        assertEquals(settings, result?.settings)
        assertEquals(listOf("schedule:exact"), scheduler.calls)
    }

    @Test
    fun `alarm trigger skips when hourly announcements are disabled`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = false, exactAlarmsEnabled = true)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler, fakeCapability(granted = true))

        val result = policy.onAlarmTriggered()

        assertNull(result)
        assertTrue(scheduler.calls.isEmpty())
    }

    private class FakeHourlyScheduleSettingsStore(initial: AppSettings) : HourlyScheduleSettingsStore {
        private val state = MutableStateFlow(initial)

        override val settings = state

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            state.value = transform(state.value)
        }
    }

    private class FakeHourlyAlarmScheduler : HourlyAlarmScheduler {
        val calls = mutableListOf<String>()

        override fun scheduleNextHour(exact: Boolean) {
            calls += if (exact) "schedule:exact" else "schedule:inexact"
        }

        override fun cancelHourlyAlarms() {
            calls += "cancel"
        }
    }
}
