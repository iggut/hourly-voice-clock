package com.hourlyvoiceclock.announcer

import java.time.LocalTime

object QuietHoursPolicy {

    fun isQuietTime(
        now: LocalTime,
        quietHoursEnabled: Boolean,
        quietStart: LocalTime,
        quietEnd: LocalTime
    ): Boolean {
        if (!quietHoursEnabled) return false

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
        allowManualDuringQuiet: Boolean
    ): Boolean {
        if (!quietHoursEnabled) return true
        val inQuiet = isQuietTime(now, true, quietStart, quietEnd)
        return if (inQuiet) allowManualDuringQuiet else true
    }
}
