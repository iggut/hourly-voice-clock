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

    @Test
    fun `enable hourly announcements schedules using the current exact setting`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = false, exactAlarmsEnabled = true)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler) { true }

        val result = policy.setEnabled(true)

        assertTrue(result.scheduledExact)
        assertEquals(listOf("schedule:exact"), scheduler.calls)
        assertTrue(store.settings.first().hourlyAnnouncementsEnabled)
    }

    @Test
    fun `enable exact alarms without permission keeps the current alarm and flags permission`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = false)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler) { false }

        val result = policy.setExactRequested(true)

        assertTrue(result.needsExactPermission)
        assertFalse(result.scheduledExact)
        assertTrue(scheduler.calls.isEmpty())
        assertTrue(store.settings.first().exactAlarmsEnabled)
    }

    @Test
    fun `boot reconciliation schedules from current policy`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = true)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler) { true }

        val result = policy.applyCurrentPolicy(ScheduleReason.BOOT)

        assertTrue(result.scheduledExact)
        assertEquals(listOf("schedule:exact"), scheduler.calls)
    }

    @Test
    fun `time change reconciliation cancels first and then reschedules`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = false)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler) { true }

        val result = policy.applyCurrentPolicy(ScheduleReason.TIME_CHANGED)

        assertFalse(result.scheduledExact)
        assertEquals(listOf("cancel", "schedule:inexact"), scheduler.calls)
    }

    @Test
    fun `alarm trigger reschedules before returning settings`() = runBlocking {
        val settings = AppSettings(hourlyAnnouncementsEnabled = true, exactAlarmsEnabled = false)
        val store = FakeHourlyScheduleSettingsStore(settings)
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler) { true }

        val result = policy.onAlarmTriggered()

        assertEquals(settings, result?.settings)
        assertEquals(listOf("schedule:inexact"), scheduler.calls)
    }

    @Test
    fun `alarm trigger skips when hourly announcements are disabled`() = runBlocking {
        val store = FakeHourlyScheduleSettingsStore(
            AppSettings(hourlyAnnouncementsEnabled = false, exactAlarmsEnabled = true)
        )
        val scheduler = FakeHourlyAlarmScheduler()
        val policy = HourlySchedulePolicy(store, scheduler) { true }

        val result = policy.onAlarmTriggered()

        assertNull(result)
        assertTrue(scheduler.calls.isEmpty())
    }

    private class FakeHourlyScheduleSettingsStore(initial: AppSettings) : HourlyScheduleSettingsStore {
        private val state = MutableStateFlow(initial)

        override val settings = state

        override suspend fun setHourlyAnnouncements(enabled: Boolean) {
            state.value = state.value.copy(hourlyAnnouncementsEnabled = enabled)
        }

        override suspend fun setExactAlarmsEnabled(enabled: Boolean) {
            state.value = state.value.copy(exactAlarmsEnabled = enabled)
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
