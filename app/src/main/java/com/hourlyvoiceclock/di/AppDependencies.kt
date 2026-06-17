package com.hourlyvoiceclock.di

import android.content.Context
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.announcer.AnnouncementNotifier
import com.hourlyvoiceclock.announcer.ChimePlayer
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.DefaultUpdateManager
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.data.UpdateManager
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.scheduler.AlarmPermissionChecker
import com.hourlyvoiceclock.scheduler.HourlySchedulePolicy
import com.hourlyvoiceclock.tts.AndroidTtsEngine
import com.hourlyvoiceclock.tts.AndroidTtsPackageProbe
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineSelector
import com.hourlyvoiceclock.tts.local.LocalTtsEngine
import com.hourlyvoiceclock.tts.local.LocalVoicesStore
import kotlinx.coroutines.SupervisorJob

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

    val ttsEngineSelector: TtsEngineSelector by lazy {
        TtsEngineSelector(
            settings = settingsRepository,
            packageProbe = AndroidTtsPackageProbe(appContext)
        )
    }

    val announcementScheduler: AnnouncementScheduler by lazy {
        AnnouncementScheduler(appContext)
    }

    val hourlySchedulePolicy: HourlySchedulePolicy by lazy {
        HourlySchedulePolicy(
            settingsStore = settingsRepository,
            scheduler = announcementScheduler,
            canScheduleExactAlarms = { AlarmPermissionChecker.canScheduleExactAlarms(appContext) }
        )
    }

    val notifier: AnnouncementNotifier by lazy {
        AnnouncementNotifier(
            context = appContext,
            channelId = HourlyVoiceClockApp.CHANNEL_ID_STATUS
        )
    }

    val chimePlayer: ChimePlayer by lazy {
        ChimePlayer(appContext)
    }

    /**
     * Process-wide cache of downloaded on-device voices. Shared
     * between the voice settings screen (which lists the available
     * models) and the dedicated Local Voices screen (which downloads
     * and deletes them).
     */
    val localVoicesStore: LocalVoicesStore by lazy {
        LocalVoicesStore()
    }

    val timeAnnouncer: TimeAnnouncer by lazy {
        TimeAnnouncer(
            context = appContext,
            ttsEngine = ttsEngine,
            chimePlayer = chimePlayer,
            notifier = notifier
        )
    }

    val updateManager: UpdateManager by lazy {
        DefaultUpdateManager(
            scope = kotlinx.coroutines.CoroutineScope(SupervisorJob()),
            appContext = appContext
        )
    }
}
