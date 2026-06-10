package com.hourlyvoiceclock.announcer

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Posts a status notification each time the app announces the time.
 *
 * Centralises the SDK 33+ permission check, the channel-id choice, the
 * title/icon, and the [NotificationManager.notify] call so the announcer
 * does not have to import any of those concerns (and does not need to
 * reach across to [com.hourlyvoiceclock.HourlyVoiceClockApp] for the
 * channel id).
 *
 * Behaviour: if the runtime permission has not been granted on Android 13+
 * the call is a silent no-op. The notifier never throws.
 */
class AnnouncementNotifier(
    private val context: Context,
    private val channelId: String,
    private val notificationId: Int = DEFAULT_NOTIFICATION_ID
) {

    fun post(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Notification logging enabled but POST_NOTIFICATIONS is denied")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val TAG = "AnnouncementNotifier"
        private const val NOTIFICATION_TITLE = "Time Announced"
        const val DEFAULT_NOTIFICATION_ID = 2001
    }
}
