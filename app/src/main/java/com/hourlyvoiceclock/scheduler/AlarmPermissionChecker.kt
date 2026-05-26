package com.hourlyvoiceclock.scheduler

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object AlarmPermissionChecker {

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isExactAlarmSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * Whether the user needs to grant the exact alarm permission via system settings.
     * Only relevant on Android 12+ (API 31+).
     */
    fun requiresUserGrant(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Whether the ACTION_REQUEST_SCHEDULE_EXACT_ALARM intent is available
     * for deep-linking. Available on API 31+.
     */
    fun exactAlarmSettingIntentAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Build an Intent to open the exact alarm permission settings.
     * On API 31+, deep-links to the specific alarm permission page with package URI.
     * Falls back to generic application settings on older versions.
     */
    fun buildExactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        }
    }

    // ── Device-specific guidance ────────────────────────────────────────────

    data class DeviceGuidance(
        val manufacturerLabel: String,
        val permissionPath: String,
        val extraNote: String?
    )

    fun getDeviceGuidance(): DeviceGuidance {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> DeviceGuidance(
                manufacturerLabel = "Samsung",
                permissionPath = "Settings → Apps → Hourly Voice Clock → Alarms & reminders",
                extraNote = "On Samsung devices, also check that Battery → Background usage limits is not restricting the app."
            )
            manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") -> DeviceGuidance(
                manufacturerLabel = "Xiaomi",
                permissionPath = "Settings → Apps → Manage Apps → Hourly Voice Clock → Other Permissions → Alarms & reminders",
                extraNote = "Xiaomi/Redmi/POCO devices may also require: Settings → Apps → Permissions → Autostart (enable), and Battery Saver → No restrictions for this app."
            )
            manufacturer.contains("huawei") ||
                manufacturer.contains("honor") -> DeviceGuidance(
                manufacturerLabel = "Huawei/Honor",
                permissionPath = "Settings → Apps → Apps → Hourly Voice Clock → Permissions → Alarms & reminders",
                extraNote = "On Huawei devices, ensure the app is not restricted under Battery optimization and Launch settings."
            )
            manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("oneplus") -> DeviceGuidance(
                manufacturerLabel = "Oppo/OnePlus",
                permissionPath = "Settings → Apps → App Management → Hourly Voice Clock → Permissions → Alarms & reminders",
                extraNote = "Oppo/OnePlus devices: enable Auto Launch and disable Battery Optimization for reliable alarms."
            )
            manufacturer.contains("vivo") -> DeviceGuidance(
                manufacturerLabel = "Vivo",
                permissionPath = "Settings → Apps → App Manager → Hourly Voice Clock → Permissions → Alarms & reminders",
                extraNote = "On Vivo devices, also check iManager → App Manager → Auto-start Manager and enable Hourly Voice Clock."
            )
            else -> DeviceGuidance(
                manufacturerLabel = "Android",
                permissionPath = "Settings → Apps → Hourly Voice Clock → Alarms & reminders",
                extraNote = "If the permission toggle is greyed out, you may need to disable battery optimization for this app first under Settings → Apps → Hourly Voice Clock → Battery."
            )
        }
    }
}
