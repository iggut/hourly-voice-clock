package com.hourlyvoiceclock.ui.home

import com.hourlyvoiceclock.scheduler.NextAnnouncementCalculator
import com.hourlyvoiceclock.scheduler.TopOfHourCalculator
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Pure domain object for formatting clock and date displays.
 *
 * Extracting the formatting logic from [HomeViewModel] makes it unit-testable
 * without Android, removes mutable caches from the ViewModel, and gives the
 * ticker a single reason to change: producing a [LocalDateTime].
 */
class Clock(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.getDefault(),
    private val nextAnnouncementCalculator: NextAnnouncementCalculator = TopOfHourCalculator()
) {

    fun now(): LocalDateTime = LocalDateTime.now(zoneId)

    fun timeState(now: LocalDateTime): TimeDisplayState {
        return TimeDisplayState(
            hoursMinutes = hoursMinutesText(now),
            seconds = secondsText(now),
            amPm = amPmText(now)
        )
    }

    fun hoursMinutesText(now: LocalDateTime): String = now.format(hoursMinutesFormatter)

    fun secondsText(now: LocalDateTime): String = SECONDS_CACHE[now.second]

    fun amPmText(now: LocalDateTime): String = now.format(amPmFormatter)

    fun dateText(now: LocalDateTime): String = now.format(dateFormatter)

    fun nextAnnouncementText(now: LocalDateTime, enabled: Boolean): String {
        if (!enabled) return ""
        val next = nextAnnouncementCalculator.nextAnnouncementTime(now)
        return next.format(nextAnnouncementFormatter)
    }

    companion object {
        private val SECONDS_CACHE = Array(60) { it.toString().padStart(2, '0') }
    }

    private val hoursMinutesFormatter = DateTimeFormatter.ofPattern("h:mm", locale)
    private val amPmFormatter = DateTimeFormatterBuilder()
        .appendText(ChronoField.AMPM_OF_DAY, mapOf(0L to "AM", 1L to "PM"))
        .toFormatter(locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
    private val nextAnnouncementFormatter = DateTimeFormatterBuilder()
        .appendPattern("h:mm ")
        .appendText(ChronoField.AMPM_OF_DAY, mapOf(0L to "AM", 1L to "PM"))
        .toFormatter(locale)

}
