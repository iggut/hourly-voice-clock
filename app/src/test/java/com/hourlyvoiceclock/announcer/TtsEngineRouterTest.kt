package com.hourlyvoiceclock.announcer

import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineInfo
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.VoiceProfile
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests — avoids Robolectric / VoiceModelRegistry resource init.
 * Android Log is stubbed via unitTests.returnDefaultValues in this module
 * for methods that may still be reached; these tests inject known-model
 * and download checks so Log paths are exercised only when needed.
 */
class TtsEngineRouterTest {

    @Test
    fun `falls back to primary when local model is not downloaded`() {
        val primary = RecordingEngine("primary")
        val local = RecordingEngine("local")
        val router = TtsEngineRouter(
            primaryEngine = primary,
            localEngineFactory = { local },
            isKnownModel = { it == "known-model" },
            isLocalModelDownloaded = { false }
        )

        val engine = router.resolveFor(AppSettings(selectedLocalModelId = "known-model"))

        assertSame(primary, engine)
        assertTrue(local.setVoiceCalls.isEmpty())
    }

    @Test
    fun `uses local engine when model is known and downloaded`() {
        val primary = RecordingEngine("primary")
        val local = RecordingEngine("local")
        val router = TtsEngineRouter(
            primaryEngine = primary,
            localEngineFactory = { local },
            isKnownModel = { it == "known-model" },
            isLocalModelDownloaded = { it == "known-model" }
        )
        local.setVoiceResult = true

        val engine = router.resolveFor(AppSettings(selectedLocalModelId = "known-model"))

        assertSame(local, engine)
        assertTrue(local.setVoiceCalls.contains("known-model"))
    }

    @Test
    fun `falls back when model id is unknown`() {
        val primary = RecordingEngine("primary")
        val local = RecordingEngine("local")
        val router = TtsEngineRouter(
            primaryEngine = primary,
            localEngineFactory = { local },
            isKnownModel = { false },
            isLocalModelDownloaded = { true }
        )

        val engine = router.resolveFor(AppSettings(selectedLocalModelId = "not-a-real-model"))

        assertSame(primary, engine)
    }

    private class RecordingEngine(private val label: String) : TtsEngine {
        var setVoiceResult: Boolean = true
        val setVoiceCalls = mutableListOf<String>()

        override suspend fun initialize(enginePackage: String?): Boolean = true
        override fun isAvailable(): Boolean = true
        override fun getVoices(): List<VoiceInfo> = emptyList()
        override fun setVoice(voiceName: String, localeTag: String): Boolean {
            setVoiceCalls += voiceName
            return setVoiceResult
        }
        override fun setLanguage(localeTag: String): Boolean = true
        override fun setPitch(pitch: Float) = Unit
        override fun setSpeechRate(rate: Float) = Unit
        override fun setAudioChannel(channel: AudioChannel) = Unit
        override fun configure(profile: VoiceProfile): Boolean = true
        override fun speak(text: String, utteranceId: String) = Unit
        override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) = onComplete(true)
        override fun stop() = Unit
        override fun shutdown() = Unit
        override suspend fun switchEngine(enginePackage: String?): Boolean = true
        override fun getEngines(): List<TtsEngineInfo> = emptyList()
        override fun getCurrentEnginePackage(): String? = null
        override fun isEspeakNgEngine(): Boolean = false
        override fun toString(): String = label
    }
}
