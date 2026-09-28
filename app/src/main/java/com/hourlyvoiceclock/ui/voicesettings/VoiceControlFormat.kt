package com.hourlyvoiceclock.ui.voicesettings

import kotlin.math.roundToInt

/**
 * Pitch and rate labels keep a tenth when the value lands on one, and
 * hundredths otherwise, so a 0.55 preset does not display as 0.6.
 */
internal fun formatVoiceControl(value: Float): String {
    val hundredths = (value * 100f).roundToInt().coerceIn(0, 999)
    val whole = hundredths / 100
    val frac = hundredths % 100
    return when {
        frac == 0 -> "$whole.0"
        frac % 10 == 0 -> "$whole.${frac / 10}"
        else -> "$whole.${frac.toString().padStart(2, '0')}"
    }
}
