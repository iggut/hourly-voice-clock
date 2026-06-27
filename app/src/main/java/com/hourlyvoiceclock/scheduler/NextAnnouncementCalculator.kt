package com.hourlyvoiceclock.scheduler

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Calculates the next time an announcement should fire.
 *
 * Isolates the scheduling policy from display formatting so [Clock]
 * does not depend on [AnnouncementScheduler].
 */
interface NextAnnouncementCalculator {
    fun nextAnnouncementTime(now: LocalDateTime): LocalDateTime
}

class TopOfHourCalculator : NextAnnouncementCalculator {
    override fun nextAnnouncementTime(now: LocalDateTime): LocalDateTime {
        return now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
    }
}
