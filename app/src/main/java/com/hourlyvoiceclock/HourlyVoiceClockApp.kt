package com.hourlyvoiceclock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.tts.AndroidTtsEngine
import com.hourlyvoiceclock.tts.TtsEngine
import kotlinx.coroutines.runBlocking

class HourlyVoiceClockApp : Application() {

    lateinit var ttsEngine: TtsEngine
        private set

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            SettingsRepository(this@HourlyVoiceClockApp).runMigrations()
        }
        ttsEngine = AndroidTtsEngine(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_STATUS,
                "Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Scheduling and status notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_STATUS = "hourly_voice_clock_status"
    }
}
