package com.hourlyvoiceclock

import com.hourlyvoiceclock.announcer.AnnouncementFormatter
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AnnouncementFormatterTest {

    @Test
    fun `simple 12 hour format`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.SIMPLE)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("It is 3 PM.", result)
    }

    @Test
    fun `simple 12 hour with minutes`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 45)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.SIMPLE)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("It is 3:45 PM.", result)
    }

    @Test
    fun `detailed 12 hour format`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.DETAILED)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("The time is 3:00 PM.", result)
    }

    @Test
    fun `friendly afternoon`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.FRIENDLY)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Good afternoon. It is 3 PM.", result)
    }

    @Test
    fun `friendly morning`() {
        val dt = LocalDateTime.of(2024, 1, 15, 9, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.FRIENDLY)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Good morning. It is 9 AM.", result)
    }

    @Test
    fun `friendly evening`() {
        val dt = LocalDateTime.of(2024, 1, 15, 20, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.FRIENDLY)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Good evening. It is 8 PM.", result)
    }

    @Test
    fun `friendly late night`() {
        val dt = LocalDateTime.of(2024, 1, 15, 3, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.FRIENDLY)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Good night. It is 3 AM.", result)
    }

    @Test
    fun `friendly late evening`() {
        val dt = LocalDateTime.of(2024, 1, 15, 23, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.FRIENDLY)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Good night. It is 11 PM.", result)
    }

    @Test
    fun `friendly early morning`() {
        val dt = LocalDateTime.of(2024, 1, 15, 4, 30)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.FRIENDLY)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Good night. It is 4:30 AM.", result)
    }

    @Test
    fun `24 hour format`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 30)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_24, phraseStyle = PhraseStyle.SIMPLE)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("It is 15:30.", result)
    }

    @Test
    fun `24 hour midnight`() {
        val dt = LocalDateTime.of(2024, 1, 15, 0, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_24, phraseStyle = PhraseStyle.SIMPLE)
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("It is 00:00.", result)
    }

    @Test
    fun `with date included`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.SIMPLE)
        val result = AnnouncementFormatter.format(dt, settings, true)
        assertEquals("It is 3 PM. Today is Monday, January 15.", result)
    }

    @Test
    fun `custom text`() {
        val dt = LocalDateTime.of(2024, 1, 15, 15, 0)
        val settings = AppSettings(timeFormat = TimeFormat.HOUR_12, phraseStyle = PhraseStyle.CUSTOM, customPrefix = "Hello there, the time is currently ", customSuffix = " master")
        val result = AnnouncementFormatter.format(dt, settings, false)
        assertEquals("Hello there, the time is currently 3 PM master", result)
    }
}
