package com.hourlyvoiceclock.data

import kotlinx.coroutines.flow.Flow

interface HourlyScheduleSettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setHourlyAnnouncements(enabled: Boolean)

    suspend fun setExactAlarmsEnabled(enabled: Boolean)
}
