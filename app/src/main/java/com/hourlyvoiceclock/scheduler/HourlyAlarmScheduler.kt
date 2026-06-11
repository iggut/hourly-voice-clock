package com.hourlyvoiceclock.scheduler

interface HourlyAlarmScheduler {
    fun scheduleNextHour(exact: Boolean)

    fun cancelHourlyAlarms()
}
