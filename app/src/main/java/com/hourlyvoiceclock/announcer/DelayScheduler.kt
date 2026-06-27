package com.hourlyvoiceclock.announcer

import android.os.Handler
import android.os.Looper

/**
 * Port for scheduling a deferred action. Abstracting the Handler lets tests
 * advance time deterministically instead of sleeping on the main looper.
 */
interface DelayScheduler {

    /**
     * Schedules [action] to run after [delayMs] milliseconds. Returns a
     * handle that can be used to cancel the pending action.
     */
    fun schedule(delayMs: Long, action: () -> Unit): ScheduledAction

    interface ScheduledAction {
        fun cancel()
    }
}

/**
 * Production implementation backed by the main-thread [Handler].
 */
class HandlerDelayScheduler : DelayScheduler {

    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(delayMs: Long, action: () -> Unit): DelayScheduler.ScheduledAction {
        val runnable = Runnable { action() }
        handler.postDelayed(runnable, delayMs)
        return object : DelayScheduler.ScheduledAction {
            override fun cancel() {
                handler.removeCallbacks(runnable)
            }
        }
    }
}
