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
        currentDay: DayOfWeek? = null
    ): Boolean {
        if (!quietHoursEnabled) return false

        if (quietDaysDisabled.isNotEmpty() && currentDay != null) {
            if (currentDay in quietDaysDisabled) return true
        }

        if (quietStart == quietEnd) return true

        return if (quietStart.isBefore(quietEnd)) {
            (now == quietStart || now.isAfter(quietStart)) && now.isBefore(quietEnd)
        } else {
            (now == quietStart || now.isAfter(quietStart)) || now.isBefore(quietEnd)
        }
    }

    fun canAnnounceManually(
        now: LocalTime,
        quietHoursEnabled: Boolean,
        quietStart: LocalTime,
        quietEnd: LocalTime,
        allowManualDuringQuiet: Boolean,
        quietDaysDisabled: Set<DayOfWeek> = emptySet(),
        currentDay: DayOfWeek? = null
    ): Boolean {
        if (quietDaysDisabled.isNotEmpty() && currentDay != null) {
            if (currentDay in quietDaysDisabled) return false
        }
        if (!quietHoursEnabled) return true
        val inQuiet = isQuietTime(now, true, quietStart, quietEnd, quietDaysDisabled, currentDay)
        return if (inQuiet) allowManualDuringQuiet else true
    }
}
