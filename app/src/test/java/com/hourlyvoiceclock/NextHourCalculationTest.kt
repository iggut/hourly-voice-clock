package com.hourlyvoiceclock

import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class NextHourCalculationTest {

    @Test
    fun `next top of hour from middle of hour`() {
        val from = LocalDateTime.of(2024, 1, 15, 14, 30, 0)
        val result = AnnouncementScheduler.getNextTopOfHour(from)
        assertEquals(LocalDateTime.of(2024, 1, 15, 15, 0, 0), result)
    }

    @Test
    fun `next top of hour from exactly top of hour`() {
        val from = LocalDateTime.of(2024, 1, 15, 14, 0, 0)
        val result = AnnouncementScheduler.getNextTopOfHour(from)
        assertEquals(LocalDateTime.of(2024, 1, 15, 15, 0, 0), result)
    }

    @Test
    fun `next top of hour crosses midnight`() {
        val from = LocalDateTime.of(2024, 1, 15, 23, 45, 0)
        val result = AnnouncementScheduler.getNextTopOfHour(from)
        assertEquals(LocalDateTime.of(2024, 1, 16, 0, 0, 0), result)
    }

    @Test
    fun `next top of hour from just after top`() {
        val from = LocalDateTime.of(2024, 1, 15, 14, 1, 0)
        val result = AnnouncementScheduler.getNextTopOfHour(from)
        assertEquals(LocalDateTime.of(2024, 1, 15, 15, 0, 0), result)
    }

    @Test
    fun `next top of hour from just before top`() {
        val from = LocalDateTime.of(2024, 1, 15, 14, 59, 59)
        val result = AnnouncementScheduler.getNextTopOfHour(from)
        assertEquals(LocalDateTime.of(2024, 1, 15, 15, 0, 0), result)
    }
}
