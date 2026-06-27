package com.hourlyvoiceclock.announcer

import androidx.test.core.app.ApplicationProvider
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.VoiceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class TimeAnnouncerTest {

    private lateinit var primaryEngine: FakeTtsEngine
    private lateinit var localEngine: FakeTtsEngine
    private lateinit var chimePlayer: FakeChimePlayer
    private lateinit var notifier: FakeAnnouncementNotifier
    private lateinit var hapticPulse: FakeHapticPulse
    private lateinit var audioFocusController: FakeAudioFocusController
    private lateinit var volumeChecker: FakeVolumeChecker
    private lateinit var userFeedback: FakeUserFeedback
    private lateinit var delayScheduler: FakeDelayScheduler
    private lateinit var announcer: TimeAnnouncer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        primaryEngine = FakeTtsEngine("primary")
        localEngine = FakeTtsEngine("local")
        chimePlayer = FakeChimePlayer(context)
        notifier = FakeAnnouncementNotifier(context)
        hapticPulse = FakeHapticPulse(context)
        audioFocusController = FakeAudioFocusController(context)
        volumeChecker = FakeVolumeChecker()
        userFeedback = FakeUserFeedback()
        delayScheduler = FakeDelayScheduler()

        val router = TtsEngineRouter(
            primaryEngine = primaryEngine,
            localEngineFactory = { localEngine }
        )

        announcer = TimeAnnouncer(
            ttsEngine = primaryEngine,
            chimePlayer = chimePlayer,
            notifier = notifier,
            hapticPulse = hapticPulse,
            audioFocusController = audioFocusController,
            ttsEngineRouter = router,
            volumeChecker = volumeChecker,
            userFeedback = userFeedback,
            delayScheduler = delayScheduler
        )
    }

    @Test
    fun `blocked by quiet hours completes false without speaking`() {
        val settings = baseSettings.copy(
            quietHoursEnabled = true,
            quietHoursStart = LocalDateTime.of(2026, 1, 1, 22, 0).toLocalTime(),
            quietHoursEnd = LocalDateTime.of(2026, 1, 1, 7, 0).toLocalTime()
        )
        val dateTime = LocalDateTime.of(2026, 1, 1, 23, 0)

        var completed = false
        announcer.announceAt(settings, dateTime = dateTime) { completed = it }

        assertFalse(completed)
        assertFalse(primaryEngine.spoke)
        assertFalse(localEngine.spoke)
        assertFalse(chimePlayer.played)
        assertFalse(hapticPulse.pulsed)
    }

    @Test
    fun `force bypasses quiet hours`() {
        val settings = baseSettings.copy(
            quietHoursEnabled = true,
            quietHoursStart = LocalDateTime.of(2026, 1, 1, 22, 0).toLocalTime(),
            quietHoursEnd = LocalDateTime.of(2026, 1, 1, 7, 0).toLocalTime()
        )
        val dateTime = LocalDateTime.of(2026, 1, 1, 23, 0)

        announcer.announceAt(settings, force = true, dateTime = dateTime) {}
        delayScheduler.runAll()

        assertTrue(primaryEngine.spoke)
    }

    @Test
    fun `muted stream shows feedback and does not speak`() {
        volumeChecker.muted = true

        announcer.announceAt(baseSettings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}

        assertTrue(userFeedback.mutedMessageShown)
        assertEquals("Media", userFeedback.lastChannelLabel)
        assertFalse(primaryEngine.spoke)
    }

    @Test
    fun `speaks through primary engine after delay`() {
        announcer.announceAt(baseSettings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}

        assertFalse(primaryEngine.spoke)
        delayScheduler.runAll()

        assertTrue(primaryEngine.spoke)
        assertTrue(audioFocusController.acquired)
        assertFalse(localEngine.spoke)
    }

    @Test
    fun `routes to local engine when local model is selected`() {
        localEngine.available = true
        val settings = baseSettings.copy(
            selectedLocalModelId = "piper_en_us_amy_medium",
            selectedLocale = "en-US"
        )

        announcer.announceAt(settings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}
        delayScheduler.runAll()

        assertTrue(localEngine.spoke)
        assertFalse(primaryEngine.spoke)
    }

    @Test
    fun `configures the selected engine with the user's voice profile`() {
        localEngine.available = true
        val settings = baseSettings.copy(
            selectedLocalModelId = "piper_en_us_amy_medium",
            selectedLocale = "en-US",
            selectedVoiceName = "samantha",
            pitch = 1.2f,
            speechRate = 0.8f,
            audioChannel = AudioChannel.NOTIFICATION
        )

        announcer.announceAt(settings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}
        delayScheduler.runAll()

        val profile = localEngine.configuredProfile
        assertEquals("samantha", profile?.voiceName)
        assertEquals("en-US", profile?.localeTag)
        assertEquals("piper_en_us_amy_medium", profile?.localModelId)
        assertEquals(1.2f, profile?.pitch)
        assertEquals(0.8f, profile?.speechRate)
        assertEquals(AudioChannel.NOTIFICATION, profile?.audioChannel)
    }

    @Test
    fun `falls back to primary engine when local model fails`() {
        localEngine.available = true
        localEngine.voiceSetFails = true
        val settings = baseSettings.copy(
            selectedLocalModelId = "piper_en_us_amy_medium",
            selectedLocale = "en-US"
        )

        announcer.announceAt(settings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}
        delayScheduler.runAll()

        assertTrue(primaryEngine.spoke)
        assertTrue(localEngine.setVoiceCalled)
    }

    @Test
    fun `plays chime before speaking`() {
        val settings = baseSettings.copy(chimeSound = ChimeSound.BELL)

        announcer.announceAt(settings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}

        assertTrue(chimePlayer.played)
        assertFalse(primaryEngine.spoke)

        chimePlayer.complete()
        delayScheduler.runAll()

        assertTrue(primaryEngine.spoke)
    }

    @Test
    fun `vibrates when configured`() {
        val settings = baseSettings.copy(vibrateBefore = true)

        announcer.announceAt(settings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}

        assertTrue(hapticPulse.pulsed)
    }

    @Test
    fun `posts notification when logging enabled`() {
        val settings = baseSettings.copy(notificationLogging = true)

        announcer.announceAt(settings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) {}
        delayScheduler.runAll()

        assertTrue(notifier.posted)
    }

    @Test
    fun `completion callback receives engine result`() {
        var result: Boolean? = null
        announcer.announceAt(baseSettings, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0)) { result = it }
        delayScheduler.runAll()

        assertEquals(true, result)
    }

    private val baseSettings: AppSettings
        get() = AppSettings(
            hourlyAnnouncementsEnabled = true,
            audioChannel = AudioChannel.MEDIA,
            timeFormat = com.hourlyvoiceclock.data.TimeFormat.HOUR_12,
            phraseStyle = com.hourlyvoiceclock.data.PhraseStyle.SIMPLE
        )
}

private class FakeTtsEngine(private val label: String) : TtsEngine {
    var spoke = false
    var available = true
    var voiceSetFails = false
    var setVoiceCalled = false
    var configuredProfile: VoiceProfile? = null
    private var lastCallback: ((Boolean) -> Unit)? = null

    override suspend fun initialize(enginePackage: String?): Boolean = true
    override fun isAvailable(): Boolean = available
    override fun getVoices(): List<VoiceInfo> = emptyList()
    override fun setVoice(voiceName: String, localeTag: String): Boolean {
        setVoiceCalled = true
        return !voiceSetFails
    }
    override fun setLanguage(localeTag: String): Boolean = true
    override fun setPitch(pitch: Float) {}
    override fun setSpeechRate(rate: Float) {}
    override fun setAudioChannel(channel: AudioChannel) {}
    override fun configure(profile: VoiceProfile): Boolean {
        configuredProfile = profile
        return true
    }
    override fun speak(text: String, utteranceId: String) {}
    override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {
        spoke = true
        lastCallback = onComplete
        onComplete(true)
    }
    override fun stop() {}
    override fun shutdown() {}
    override suspend fun switchEngine(enginePackage: String?): Boolean = true
    override fun getEngines(): List<com.hourlyvoiceclock.tts.TtsEngineInfo> = emptyList()
    override fun getCurrentEnginePackage(): String? = null
    override fun isEspeakNgEngine(): Boolean = false
}

private class FakeChimePlayer(context: android.content.Context) : ChimePlayer(context) {
    var played = false
    private var pendingComplete: (() -> Unit)? = null

    override fun play(sound: ChimeSound, onComplete: () -> Unit) {
        played = true
        pendingComplete = onComplete
    }

    fun complete() {
        pendingComplete?.invoke()
    }
}

private class FakeAnnouncementNotifier(context: android.content.Context) :
    AnnouncementNotifier(context, "test-channel") {
    var posted = false
    override fun post(text: String) {
        posted = true
    }
}

private class FakeHapticPulse(context: android.content.Context) : HapticPulse(context) {
    var pulsed = false
    override fun pulse(milliseconds: Long) {
        pulsed = true
    }
}

private class FakeAudioFocusController(context: android.content.Context) :
    AudioFocusController(context) {
    var acquired = false
    override fun acquire(usage: Int, audioStream: Int): Held? {
        acquired = true
        return null
    }
}

private class FakeVolumeChecker : VolumeChecker {
    var muted = false
    override fun isMuted(audioStream: Int): Boolean = muted
    override fun currentVolume(audioStream: Int): Int = if (muted) 0 else 7
    override fun maxVolume(audioStream: Int): Int = 15
}

private class FakeUserFeedback : UserFeedback {
    var mutedMessageShown = false
    var lastChannelLabel: String? = null
    override fun showMutedStreamMessage(channelLabel: String) {
        mutedMessageShown = true
        lastChannelLabel = channelLabel
    }
}

private class FakeDelayScheduler : DelayScheduler {
    private val pending = mutableListOf<() -> Unit>()

    override fun schedule(delayMs: Long, action: () -> Unit): DelayScheduler.ScheduledAction {
        pending.add(action)
        return object : DelayScheduler.ScheduledAction {
            override fun cancel() {
                pending.remove(action)
            }
        }
    }

    fun runAll() {
        val copy = pending.toList()
        pending.clear()
        copy.forEach { it() }
    }
}
