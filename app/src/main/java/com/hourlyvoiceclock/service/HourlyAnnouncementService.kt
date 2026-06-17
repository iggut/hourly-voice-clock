package com.hourlyvoiceclock.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.MainActivity
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.di.DependenciesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.resume

/**
 * Executes scheduled hourly announcements while the device is locked, the
 * screen is off, or Doze is active.
 *
 * A BroadcastReceiver is too short-lived for this job: TimeAnnouncer posts a
 * small delayed speak call and the TTS engines complete asynchronously. Once a
 * receiver's pending result is finished, Android may suspend or kill the app
 * process before audio starts. Running the alarm work in a foreground service
 * with a partial wake lock keeps the CPU alive until TTS reports completion.
 */
class HourlyAnnouncementService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var runningStartId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_ANNOUNCE) return START_NOT_STICKY
        runningStartId = startId

        startForeground(NOTIFICATION_ID, buildNotification())
        acquireWakeLock()

        scope.launch {
            try {
                runAnnouncement()
            } catch (t: Throwable) {
                Log.e(TAG, "Scheduled announcement failed", t)
            } finally {
                releaseWakeLock()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelfResult(runningStartId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runAnnouncement() {
        val app = applicationContext as DependenciesProvider
        val deps = app.dependencies
        val result = withContext(Dispatchers.IO) {
            deps.hourlySchedulePolicy.onAlarmTriggered()
        }

        if (result == null) {
            Log.d(TAG, "Hourly announcements disabled, skipping")
            return
        }

        Log.d(TAG, "Rescheduled next hourly alarm from foreground service")

        val selectedPackage = deps.ttsEngineSelector.select()
        deps.ttsEngine.initialize(selectedPackage)
        val scheduledHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)

        val completed = awaitAnnouncementCompletion {
            deps.timeAnnouncer.announce(
                settings = result.settings,
                force = false,
                dateTime = scheduledHour,
                onComplete = it
            )
        }
        Log.d(TAG, "Scheduled announcement completed: $completed")
    }

    private suspend fun awaitAnnouncementCompletion(
        start: ((Boolean) -> Unit) -> Unit
    ): Boolean {
        var resumed = false
        val success = suspendCancellableCoroutine { continuation ->
            val timeoutJob = scope.launch {
                delay(MAX_ANNOUNCEMENT_MS)
                if (!resumed && continuation.isActive) {
                    resumed = true
                    Log.w(TAG, "Timed out waiting for announcement completion")
                    continuation.resume(false)
                }
            }
            start { result ->
                if (!resumed && continuation.isActive) {
                    resumed = true
                    timeoutJob.cancel()
                    continuation.resume(result)
                }
            }
            continuation.invokeOnCancellation { timeoutJob.cancel() }
        }
        return success
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:$TAG"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to release wake lock", t)
        } finally {
            wakeLock = null
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, HourlyVoiceClockApp.CHANNEL_ID_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hourly Voice Clock")
            .setContentText("Preparing hourly announcement")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val TAG = "HourlyAnnouncementService"
        private const val NOTIFICATION_ID = 2042
        private const val MAX_ANNOUNCEMENT_MS = 45_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 60_000L
        const val ACTION_ANNOUNCE = "com.hourlyvoiceclock.ACTION_ANNOUNCE_FROM_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, HourlyAnnouncementService::class.java)
                .setAction(ACTION_ANNOUNCE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
