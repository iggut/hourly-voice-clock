package com.hourlyvoiceclock

import com.hourlyvoiceclock.announcer.QuietHoursPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class QuietHoursPolicyTest {

    @Test
    fun `quiet hours disabled always returns false`() {
        val now = LocalTime.of(23, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(now, false, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours normal range - inside`() {
        val now = LocalTime.of(23, 0)
        assertTrue(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours normal range - outside after end`() {
        val now = LocalTime.of(8, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours normal range - outside before start`() {
        val now = LocalTime.of(21, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours crossing midnight - at start boundary`() {
        val now = LocalTime.of(22, 0)
        assertTrue(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours crossing midnight - just after start`() {
        val now = LocalTime.of(22, 1)
        assertTrue(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours crossing midnight - just before end`() {
        val now = LocalTime.of(6, 59)
        assertTrue(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours crossing midnight - at end boundary`() {
        val now = LocalTime.of(7, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
    }

    @Test
    fun `quiet hours non crossing - at start boundary`() {
        val now = LocalTime.of(9, 0)
        assertTrue(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(9, 0), LocalTime.of(17, 0))
        )
    }

    @Test
    fun `quiet hours non crossing - at end boundary`() {
        val now = LocalTime.of(17, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(9, 0), LocalTime.of(17, 0))
        )
    }

    @Test
    fun `quiet hours non crossing - inside`() {
        val now = LocalTime.of(12, 0)
        assertTrue(
            QuietHoursPolicy.isQuietTime(now, true, LocalTime.of(9, 0), LocalTime.of(17, 0))
        )
    }

    @Test
    fun `manual announcement allowed when not in quiet hours`() {
        val now = LocalTime.of(12, 0)
        assertTrue(
            QuietHoursPolicy.canAnnounceManually(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0), true
            )
        )
    }

    @Test
    fun `manual announcement allowed in quiet hours when setting enabled`() {
        val now = LocalTime.of(23, 0)
        assertTrue(
            QuietHoursPolicy.canAnnounceManually(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0), true
            )
        )
    }

    @Test
    fun `manual announcement blocked in quiet hours when setting disabled`() {
        val now = LocalTime.of(23, 0)
        assertFalse(
            QuietHoursPolicy.canAnnounceManually(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0), false
            )
        )
    }

    @Test
    fun `disabled day suppresses announcements regardless of time`() {
        val now = LocalTime.of(12, 0)
        assertTrue(
            QuietHoursPolicy.isQuietTime(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0),
                quietDaysDisabled = setOf(DayOfWeek.WEDNESDAY),
                currentDay = DayOfWeek.WEDNESDAY
            )
        )
    }

    @Test
    fun `non-disabled day does not suppress outside quiet hours`() {
        val now = LocalTime.of(12, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0),
                quietDaysDisabled = setOf(DayOfWeek.WEDNESDAY),
                currentDay = DayOfWeek.THURSDAY
            )
        )
    }

    @Test
    fun `manual announcement blocked on disabled day`() {
        val now = LocalTime.of(12, 0)
        assertFalse(
            QuietHoursPolicy.canAnnounceManually(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0),
                allowManualDuringQuiet = true,
                quietDaysDisabled = setOf(DayOfWeek.SATURDAY),
                currentDay = DayOfWeek.SATURDAY
            )
        )
    }

    @Test
    fun `manual announcement allowed on non-disabled day outside quiet hours`() {
        val now = LocalTime.of(12, 0)
        assertTrue(
            QuietHoursPolicy.canAnnounceManually(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0),
                allowManualDuringQuiet = true,
                quietDaysDisabled = setOf(DayOfWeek.SATURDAY),
                currentDay = DayOfWeek.MONDAY
            )
        )
    }

    @Test
    fun `empty disabled days has no effect`() {
        val now = LocalTime.of(12, 0)
        assertFalse(
            QuietHoursPolicy.isQuietTime(
                now, true, LocalTime.of(22, 0), LocalTime.of(7, 0),
                quietDaysDisabled = emptySet(),
                currentDay = DayOfWeek.MONDAY
            )
        )
    }
}
