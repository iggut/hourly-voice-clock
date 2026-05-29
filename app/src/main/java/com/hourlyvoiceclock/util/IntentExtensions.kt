package com.hourlyvoiceclock.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(intent)
}

fun Context.openIgnoreBatteryOptimizationSettings() {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    startActivity(intent)
}

fun Context.openTtsSettings() {
    val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

fun Context.openTtsEngineSettings(enginePackage: String) {
    val intent = Intent("android.settings.TTS_SETTINGS").apply {
        putExtra("android.speech.extras.EXTRA_TTS_ENGINE_PACKAGE_NAME", enginePackage)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        startActivity(intent)
    } catch (e: Exception) {
        // Fallback to generic TTS settings
        openTtsSettings()
    }
}

fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}
