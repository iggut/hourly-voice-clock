package com.hourlyvoiceclock.data

import kotlinx.coroutines.flow.Flow

/**
 * Narrow seam used by [com.hourlyvoiceclock.scheduler.HourlySchedulePolicy].
 *
 * The policy only needs to read the current settings and apply atomic
 * transforms; individual field setters would widen the interface for no
 * benefit.
 */
interface HourlyScheduleSettingsStore {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
