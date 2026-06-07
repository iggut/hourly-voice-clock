package com.hourlyvoiceclock.di

import android.content.Context
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.tts.AndroidTtsEngine
import com.hourlyvoiceclock.tts.TtsEngine

/**
 * Central composition root for shared application dependencies.
 * All long-lived singletons are constructed here once and passed
 * to ViewModels and other consumers via constructor injection.
 *
 * Created once in HourlyVoiceClockApp and exposed through DependenciesProvider.
 */
class AppDependencies(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val ttsEngine: TtsEngine by lazy {
        AndroidTtsEngine(appContext)
    }

    val announcementScheduler: AnnouncementScheduler by lazy {
        AnnouncementScheduler(appContext)
    }

    val timeAnnouncer: TimeAnnouncer by lazy {
        TimeAnnouncer(appContext, ttsEngine)
    }
}
