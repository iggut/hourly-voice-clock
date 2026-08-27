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

    // ⚡ Bolt: Precomputed minute and hour strings to prevent allocations from padStart during high-frequency formatting
    private val PADDED_NUMBERS = Array(60) { it.toString().padStart(2, '0') }

    private fun format12Hour(dateTime: LocalDateTime, alwaysShowMinutes: Boolean): String {
        val hour = dateTime.hour % 12
        val displayHour = if (hour == 0) 12 else hour
        val minute = dateTime.minute
        val amPm = if (dateTime.hour < 12) "AM" else "PM"
        return if (minute == 0 && !alwaysShowMinutes) {
            "$displayHour $amPm"
        } else {
            "$displayHour:${PADDED_NUMBERS[minute]} $amPm"
        }
    }

    private fun format24Hour(dateTime: LocalDateTime, alwaysShowMinutes: Boolean): String {
        // ⚡ Bolt: Replace String.format with string template and cached strings to prevent format parsing and GC overhead
        val hourStr = PADDED_NUMBERS[dateTime.hour]
        val minuteStr = PADDED_NUMBERS[dateTime.minute]
        return "$hourStr:$minuteStr"
    }

    private fun getGreeting(hour: Int): String {
        // Every hour of the day maps to a named greeting so the friendly
        // style never collapses into a flat "Hello." for late-night
        // users. The bands:
        //   05:00-11:59  morning
        //   12:00-16:59  afternoon
        //   17:00-21:59  evening
        //   22:00-04:59  night
        return when (hour) {
            in 5..11 -> "Good morning."
            in 12..16 -> "Good afternoon."
            in 17..21 -> "Good evening."
            else -> "Good night."
        }
    }

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
}
