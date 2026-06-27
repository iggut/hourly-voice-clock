package com.hourlyvoiceclock.di

import android.content.Context
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.announcer.AnnouncementNotifier
import com.hourlyvoiceclock.announcer.AndroidVolumeChecker
import com.hourlyvoiceclock.announcer.AudioFocusController
import com.hourlyvoiceclock.announcer.ChimePlayer
import com.hourlyvoiceclock.announcer.DelayScheduler
import com.hourlyvoiceclock.announcer.HandlerDelayScheduler
import com.hourlyvoiceclock.announcer.HapticPulse
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.announcer.ToastUserFeedback
import com.hourlyvoiceclock.announcer.TtsEngineRouter
import com.hourlyvoiceclock.announcer.UserFeedback
import com.hourlyvoiceclock.announcer.VolumeChecker
import com.hourlyvoiceclock.data.AndroidSignatureVerifier
import com.hourlyvoiceclock.data.AndroidUpdateUiDelegate
import com.hourlyvoiceclock.data.DefaultUpdateManager
import com.hourlyvoiceclock.data.GitHubUpdateChecker
import com.hourlyvoiceclock.data.OkHttpUpdateDownloader
import com.hourlyvoiceclock.data.SettingsDataStore
import com.hourlyvoiceclock.data.SettingsMapper
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.data.SignatureVerifier
import com.hourlyvoiceclock.data.UpdateChecker
import com.hourlyvoiceclock.data.UpdateDownloader
import com.hourlyvoiceclock.data.UpdateManager
import com.hourlyvoiceclock.data.UpdateUiDelegate
import com.hourlyvoiceclock.scheduler.AndroidExactAlarmCapability
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.scheduler.ExactAlarmCapability
import com.hourlyvoiceclock.scheduler.HourlySchedulePolicy
import com.hourlyvoiceclock.tts.AndroidTtsEngine
import com.hourlyvoiceclock.tts.AndroidTtsPackageProbe
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineSelector
import com.hourlyvoiceclock.tts.local.DefaultLocalVoiceRepository
import com.hourlyvoiceclock.tts.local.LocalTtsEngine
import com.hourlyvoiceclock.tts.local.LocalVoiceRepository
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

    private val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(appContext)
    }

    private val settingsMapper: SettingsMapper by lazy {
        SettingsMapper()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsDataStore, settingsMapper)
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

    val exactAlarmCapability: ExactAlarmCapability by lazy {
        AndroidExactAlarmCapability(appContext)
    }

    val hourlySchedulePolicy: HourlySchedulePolicy by lazy {
        HourlySchedulePolicy(
            settingsStore = settingsRepository,
            scheduler = announcementScheduler,
            exactAlarmCapability = exactAlarmCapability
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
     * Single source of truth for downloaded on-device voices. Shared
     * between the voice settings screen (which lists the available
     * models) and the dedicated Local Voices screen (which downloads
     * and deletes them).
     */
    val localVoiceRepository: LocalVoiceRepository by lazy {
        DefaultLocalVoiceRepository(appContext)
    }

    private val ttsEngineRouter: TtsEngineRouter by lazy {
        TtsEngineRouter(
            primaryEngine = ttsEngine,
            localEngineFactory = { LocalTtsEngine(appContext) }
        )
    }

    private val volumeChecker: VolumeChecker by lazy {
        AndroidVolumeChecker(appContext)
    }

    private val userFeedback: UserFeedback by lazy {
        ToastUserFeedback(appContext)
    }

    private val delayScheduler: DelayScheduler by lazy {
        HandlerDelayScheduler()
    }

    val timeAnnouncer: TimeAnnouncer by lazy {
        TimeAnnouncer(
            ttsEngine = ttsEngine,
            chimePlayer = chimePlayer,
            notifier = notifier,
            hapticPulse = HapticPulse(appContext),
            audioFocusController = AudioFocusController(appContext),
            ttsEngineRouter = ttsEngineRouter,
            volumeChecker = volumeChecker,
            userFeedback = userFeedback,
            delayScheduler = delayScheduler
        )
    }

    private val updateChecker: UpdateChecker by lazy {
        GitHubUpdateChecker()
    }

    private val updateDownloader: UpdateDownloader by lazy {
        OkHttpUpdateDownloader()
    }

    private val signatureVerifier: SignatureVerifier by lazy {
        AndroidSignatureVerifier(appContext)
    }

    private val updateUiDelegate: UpdateUiDelegate by lazy {
        AndroidUpdateUiDelegate(appContext)
    }

    val updateManager: UpdateManager by lazy {
        DefaultUpdateManager(
            scope = kotlinx.coroutines.CoroutineScope(SupervisorJob()),
            updateChecker = updateChecker,
            downloader = updateDownloader,
            signatureVerifier = signatureVerifier,
            uiDelegate = updateUiDelegate
        )
    }
}
