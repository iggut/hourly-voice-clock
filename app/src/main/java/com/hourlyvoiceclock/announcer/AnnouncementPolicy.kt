package com.hourlyvoiceclock.announcer

import com.hourlyvoiceclock.data.AppSettings
import java.time.LocalDateTime

/**
 * Pure policy that decides whether an announcement is currently allowed.
 *
 * The only framework-dependent part of the decision — whether the selected
 * audio stream is muted — is kept outside this object so the policy itself
 * can be tested with plain JVM tests.
 */
object AnnouncementPolicy {

    /**
     * Returns `true` when the current time falls inside the user's configured
     * quiet hours (or quiet-day hours) and [force] is `false`.
     */
    fun isBlockedByQuietHours(
        settings: AppSettings,
        dateTime: LocalDateTime,
        force: Boolean
    ): Boolean {
        if (force) return false
        return QuietHoursPolicy.isQuietTime(
            now = dateTime.toLocalTime(),
            quietHoursEnabled = settings.quietHoursEnabled,
            quietStart = settings.quietHoursStart,
            quietEnd = settings.quietHoursEnd,
            quietDaysDisabled = settings.quietDaysDisabled,
            currentDay = dateTime.dayOfWeek,
            quietDaysStart = settings.quietDaysQuietStart,
            quietDaysEnd = settings.quietDaysQuietEnd
        )
    }
}
