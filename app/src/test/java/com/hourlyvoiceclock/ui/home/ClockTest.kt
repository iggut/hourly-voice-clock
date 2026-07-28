package com.hourlyvoiceclock.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ClockTest {

    private val clock = Clock(ZoneId.of("UTC"))

    @Test
    fun `timeState formats 12-hour clock`() {
        val now = LocalDateTime.of(2026, 6, 27, 14, 5, 9)

        val state = clock.timeState(now)

        assertEquals("2:05", state.hoursMinutes)
        assertEquals("PM", state.amPm)
    }

    @Test
    fun `timeState formats midnight correctly`() {
        val now = LocalDateTime.of(2026, 6, 27, 0, 0, 0)

        val state = clock.timeState(now)

        assertEquals("12:00", state.hoursMinutes)
        assertEquals("AM", state.amPm)
    }

    @Test
    fun `secondsText returns padded seconds`() {
        val now = LocalDateTime.of(2026, 6, 27, 14, 5, 9)
        assertEquals("09", clock.secondsText(now))
    }

    @Test
    fun `dateText formats full date`() {
        val now = LocalDateTime.of(2026, 6, 27, 12, 0)

        val date = clock.dateText(now)

        assertEquals("Saturday, June 27, 2026", date)
    }

    @Test
    fun `nextAnnouncementText is empty when disabled`() {
        val now = LocalDateTime.of(2026, 6, 27, 14, 5)

        val text = clock.nextAnnouncementText(now, enabled = false)

        assertEquals("", text)
    }

    @Test
    fun `nextAnnouncementText formats next top of hour`() {
        val now = LocalDateTime.of(2026, 6, 27, 14, 5)

        val text = clock.nextAnnouncementText(now, enabled = true)

        assertEquals("3:00 PM", text)
    }
}
