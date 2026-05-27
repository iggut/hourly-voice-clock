package com.hourlyvoiceclock.announcer

import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object AnnouncementFormatter {

    fun format(
        dateTime: LocalDateTime,
        settings: com.hourlyvoiceclock.data.AppSettings,
        includeDate: Boolean
    ): String {
        val timeFormat = settings.timeFormat
        val phraseStyle = settings.phraseStyle
        val timeText = when (timeFormat) {
            TimeFormat.HOUR_12 -> format12Hour(dateTime, phraseStyle == PhraseStyle.DETAILED)
            TimeFormat.HOUR_24 -> format24Hour(dateTime, phraseStyle == PhraseStyle.DETAILED)
        }

        val greeting = when (phraseStyle) {
            PhraseStyle.FRIENDLY -> getGreeting(dateTime.hour)
            else -> null
        }

        val phrase = when (phraseStyle) {
            PhraseStyle.SIMPLE -> "It is $timeText."
            PhraseStyle.DETAILED -> "The time is $timeText."
            PhraseStyle.FRIENDLY -> "$greeting It is $timeText."
            PhraseStyle.CUSTOM -> "${settings.customPrefix}$timeText${settings.customSuffix}".trim()
        }

        return if (includeDate) {
            val dateText = dateTime.format(DATE_FORMATTER)
            "$phrase Today is $dateText."
        } else {
            phrase
        }
    }

    private fun format12Hour(dateTime: LocalDateTime, alwaysShowMinutes: Boolean): String {
        val hour = dateTime.hour % 12
        val displayHour = if (hour == 0) 12 else hour
        val minute = dateTime.minute
        val amPm = if (dateTime.hour < 12) "AM" else "PM"
        return if (minute == 0 && !alwaysShowMinutes) {
            "$displayHour $amPm"
        } else {
            String.format("%d:%02d %s", displayHour, minute, amPm)
        }
    }

    private fun format24Hour(dateTime: LocalDateTime, alwaysShowMinutes: Boolean): String {
        return String.format("%02d:%02d", dateTime.hour, dateTime.minute)
    }

    private fun getGreeting(hour: Int): String {
        return when (hour) {
            in 5..11 -> "Good morning."
            in 12..16 -> "Good afternoon."
            in 17..21 -> "Good evening."
            else -> "Hello."
        }
    }

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
}
