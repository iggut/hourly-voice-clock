package com.hourlyvoiceclock.scheduler

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Single decision point for exact-alarm scheduling capability.
 *
 * The adapter hides all Android-version branching, intent construction,
 * and device-specific guidance behind one method. Callers no longer need
 * to import [Build.VERSION] or combine raw permission facts themselves.
 */
interface ExactAlarmCapability {
    fun current(): ExactAlarmState
}

sealed interface ExactAlarmState {
    data object Granted : ExactAlarmState

    data class Denied(
        val canRequest: Boolean,
        val settingsIntent: Intent,
        val guidance: DeviceGuidance
    ) : ExactAlarmState
}

data class DeviceGuidance(
    val manufacturerLabel: String,
    val permissionPath: String,
    val extraNote: String?
)

class AndroidExactAlarmCapability(private val context: Context) : ExactAlarmCapability {

    override fun current(): ExactAlarmState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                ExactAlarmState.Granted
            } else {
                ExactAlarmState.Denied(
                    canRequest = true,
                    settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    },
                    guidance = getDeviceGuidance()
                )
            }
        } else {
            ExactAlarmState.Granted
        }
    }

    private fun getDeviceGuidance(): DeviceGuidance {
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
