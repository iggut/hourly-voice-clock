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

    // ⚡ Bolt: Cache formatted strings to avoid redundant string allocations in the 1-second view model loop
    private var lastMinuteHash: Int = -1
    private var cachedHoursMinutes: String = ""
    private var cachedAmPm: String = ""

    private var lastDateHash: Int = -1
    private var cachedDate: String = ""

    fun timeState(now: LocalDateTime): TimeDisplayState {
        val minuteHash = now.year * 400 * 24 * 60 + now.dayOfYear * 24 * 60 + now.hour * 60 + now.minute
        if (minuteHash != lastMinuteHash) {
            lastMinuteHash = minuteHash
            cachedHoursMinutes = now.format(hoursMinutesFormatter)
            cachedAmPm = now.format(amPmFormatter)
        }

        return TimeDisplayState(
            hoursMinutes = cachedHoursMinutes,
            seconds = SECONDS_CACHE[now.second],
            amPm = cachedAmPm
        )
    }

    fun dateText(now: LocalDateTime): String {
        val dateHash = now.year * 400 + now.dayOfYear
        if (dateHash != lastDateHash) {
            lastDateHash = dateHash
            cachedDate = now.format(dateFormatter)
        }
        return cachedDate
    }

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
