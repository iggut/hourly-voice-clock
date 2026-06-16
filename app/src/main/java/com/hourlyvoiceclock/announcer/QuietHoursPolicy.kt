package com.hourlyvoiceclock.announcer

import java.time.DayOfWeek
import java.time.LocalTime

object QuietHoursPolicy {

    fun isQuietTime(
        now: LocalTime,
        quietHoursEnabled: Boolean,
        quietStart: LocalTime,
        quietEnd: LocalTime,
        quietDaysDisabled: Set<DayOfWeek> = emptySet(),
        currentDay: DayOfWeek? = null,
        quietDaysStart: LocalTime = quietStart,
        quietDaysEnd: LocalTime = quietEnd
    ): Boolean {
        if (!quietHoursEnabled) return false

        val (start, end) = if (currentDay != null && currentDay in quietDaysDisabled) {
            quietDaysStart to quietDaysEnd
        } else {
            quietStart to quietEnd
        }

        if (start == end) return true

        return if (start.isBefore(end)) {
            (now == start || now.isAfter(start)) && now.isBefore(end)
        } else {
            (now == start || now.isAfter(start)) || now.isBefore(end)
        }
    }

    fun canAnnounceManually(
        now: LocalTime,
        quietHoursEnabled: Boolean,
        quietStart: LocalTime,
        quietEnd: LocalTime,
        allowManualDuringQuiet: Boolean,
        quietDaysDisabled: Set<DayOfWeek> = emptySet(),
        currentDay: DayOfWeek? = null,
        quietDaysStart: LocalTime = quietStart,
        quietDaysEnd: LocalTime = quietEnd
    ): Boolean {
        if (!quietHoursEnabled) return true
        val inQuiet = isQuietTime(
            now,
            true,
            quietStart,
            quietEnd,
            quietDaysDisabled,
            currentDay,
            quietDaysStart,
            quietDaysEnd
        )
        return if (inQuiet) allowManualDuringQuiet else true
    }
}
