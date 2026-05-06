package com.hourlyvoiceclock

import com.hourlyvoiceclock.announcer.AnnouncementFormatter
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AnnouncementFormatterTest {

    @Test
    fun `simple 12 hour format`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.SIMPLE, false)
        assertEquals("It is 3 PM.", result)
    }

    @Test
    fun `simple 12 hour with minutes`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 45)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.SIMPLE, false)
        assertEquals("It is 3:45 PM.", result)
    }

    @Test
    fun `detailed 12 hour format`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.DETAILED, false)
        assertEquals("The time is 3:00 PM.", result)
    }

    @Test
    fun `friendly afternoon`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.FRIENDLY, false)
        assertEquals("Good afternoon. It is 3 PM.", result)
    }

    @Test
    fun `friendly morning`() {
        val dt = LocalDateTime.of(2024, 1, 15, 9, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.FRIENDLY, false)
        assertEquals("Good morning. It is 9 AM.", result)
    }

    @Test
    fun `friendly evening`() {
        val dt = LocalDateTime.of(2024, 1, 15, 20, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.FRIENDLY, false)
        assertEquals("Good evening. It is 8 PM.", result)
    }

    @Test
    fun `friendly late night`() {
        val dt = LocalDateTime.of(2024, 1, 15, 3, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.FRIENDLY, false)
        assertEquals("Hello. It is 3 AM.", result)
    }

    @Test
    fun `24 hour format`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 30)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_24, PhraseStyle.SIMPLE, false)
        assertEquals("It is 15:30.", result)
    }

    @Test
    fun `24 hour midnight`() {
        val dt = LocalDateTime.of(2024, 1, 15, 0, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_24, PhraseStyle.SIMPLE, false)
        assertEquals("It is 00:00.", result)
    }

    @Test
    fun `with date included`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val result = AnnouncementFormatter.format(dt, TimeFormat.HOUR_12, PhraseStyle.SIMPLE, true)
        assertEquals("It is 3 PM. Today is Monday, January 15.", result)
    }
}
