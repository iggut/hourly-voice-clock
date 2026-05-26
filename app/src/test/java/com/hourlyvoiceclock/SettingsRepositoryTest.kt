package com.hourlyvoiceclock

import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.data.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class SettingsRepositoryTest {

    @Test
    fun `safeEnumValueOf returns matching enum for valid string`() {
        val style = SettingsRepository.safeEnumValueOf("FRIENDLY", PhraseStyle.SIMPLE)
        assertEquals(PhraseStyle.FRIENDLY, style)

        val format = SettingsRepository.safeEnumValueOf("HOUR_24", TimeFormat.HOUR_12)
        assertEquals(TimeFormat.HOUR_24, format)

        val channel = SettingsRepository.safeEnumValueOf("NOTIFICATION", AudioChannel.MEDIA)
        assertEquals(AudioChannel.NOTIFICATION, channel)
    }

    @Test
    fun `safeEnumValueOf returns fallback for invalid string`() {
        val style = SettingsRepository.safeEnumValueOf("INVALID_STYLE", PhraseStyle.SIMPLE)
        assertEquals(PhraseStyle.SIMPLE, style)

        val format = SettingsRepository.safeEnumValueOf("24HOUR", TimeFormat.HOUR_12)
        assertEquals(TimeFormat.HOUR_12, format)
    }

    @Test
    fun `safeEnumValueOf returns fallback for null string`() {
        val style = SettingsRepository.safeEnumValueOf(null, PhraseStyle.CUSTOM)
        assertEquals(PhraseStyle.CUSTOM, style)
    }

    @Test
    fun `parseTime returns correct LocalTime for valid HH mm format`() {
        val time = SettingsRepository.parseTime("08:30", "22:00")
        assertEquals(LocalTime.of(8, 30), time)

        val time2 = SettingsRepository.parseTime("23:59", "22:00")
        assertEquals(LocalTime.of(23, 59), time2)
    }

    @Test
    fun `parseTime returns fallback for null or empty value`() {
        val timeNull = SettingsRepository.parseTime(null, "07:15")
        assertEquals(LocalTime.of(7, 15), timeNull)

        val timeBlank = SettingsRepository.parseTime("   ", "12:00")
        assertEquals(LocalTime.of(12, 0), timeBlank)
    }

    @Test
    fun `parseTime returns fallback for invalid format or non-numeric values`() {
        val invalidFormat = SettingsRepository.parseTime("8-30", "09:00")
        assertEquals(LocalTime.of(9, 0), invalidFormat)

        val invalidChars = SettingsRepository.parseTime("ab:cd", "10:30")
        assertEquals(LocalTime.of(10, 30), invalidChars)

        val outOfBounds = SettingsRepository.parseTime("25:00", "11:00")
        assertEquals(LocalTime.of(11, 0), outOfBounds)
    }

    @Test
    fun `parseTime falls back to hardcoded default if fallback is also invalid`() {
        val time = SettingsRepository.parseTime("corrupted", "also_corrupted")
        assertEquals(LocalTime.of(22, 0), time)
    }

    @Test
    fun `formatTime formats LocalTime correctly`() {
        assertEquals("00:00", SettingsRepository.formatTime(LocalTime.of(0, 0)))
        assertEquals("09:05", SettingsRepository.formatTime(LocalTime.of(9, 5)))
        assertEquals("22:30", SettingsRepository.formatTime(LocalTime.of(22, 30)))
    }
}
