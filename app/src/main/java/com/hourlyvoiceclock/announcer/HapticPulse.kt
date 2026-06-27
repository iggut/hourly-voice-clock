package com.hourlyvoiceclock.announcer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Triggers a short haptic pulse before a spoken announcement.
 *
 * Owns the [Vibrator] service lookup and the pre-Oreo vs Oreo+ branching.
 * No-ops cleanly when the device has no vibrator service.
 */
open class HapticPulse(private val context: Context) {

    open fun pulse(milliseconds: Long = DEFAULT_PULSE_MS) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    companion object {
        const val DEFAULT_PULSE_MS = 200L
    }
}
