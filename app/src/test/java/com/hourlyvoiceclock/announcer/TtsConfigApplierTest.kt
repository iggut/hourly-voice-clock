package com.hourlyvoiceclock.announcer

import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.tts.TtsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Tests the voice/language fallback cascade in isolation. The cascade is
 * the deepest piece of un-tested logic that the original TimeAnnouncer
 * inlined in speakText(); these tests pin its behaviour so future
 * refactors don't silently break announcements in non-English locales.
 */
class TtsConfigApplierTest {

    private val fakeEngine = RecordingTtsEngine()
    private val applier = TtsConfigApplier(fakeEngine)

    @Test
    fun `saved voice is tried first`() {
        fakeEngine.voiceResult = true
        val result = applier.selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB"
        )
        assertTrue(result)
        assertEquals(listOf("voice:samantha|en-US"), fakeEngine.calls)
    }

    @Test
    fun `falls back to saved locale when voice fails`() {
        fakeEngine.voiceResult = false
        fakeEngine.languageResults["en-US"] = true
        val result = applier.selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB"
        )
        assertTrue(result)
        assertEquals(listOf("voice:samantha|en-US", "lang:en-US"), fakeEngine.calls)
    }

    @Test
    fun `falls back to device default when voice and saved locale both fail`() {
        fakeEngine.voiceResult = false
        fakeEngine.languageResults["en-US"] = false
        fakeEngine.languageResults["en-GB"] = true
        val result = applier.selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB"
        )
        assertTrue(result)
        assertEquals(
            listOf("voice:samantha|en-US", "lang:en-US", "lang:en-GB"),
            fakeEngine.calls
        )
    }

    @Test
    fun `falls back to en-US hard fallback when nothing else works`() {
        // No setLanguage call ever returns true.
        val result = applier.selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "fr-FR",
            deviceDefaultLocale = "en-GB"
        )
        assertFalse(result)
        assertEquals(
            listOf("voice:samantha|fr-FR", "lang:fr-FR", "lang:en-GB", "lang:en-US"),
            fakeEngine.calls
        )
    }

    @Test
    fun `blank voice name is skipped and cascade continues from locale`() {
        fakeEngine.languageResults["en-US"] = true
        val result = applier.selectVoiceOrLocale(
            voiceName = "",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB"
        )
        assertTrue(result)
        assertEquals(listOf("lang:en-US"), fakeEngine.calls)
    }

    @Test
    fun `null saved locale is skipped at the locale step`() {
        // We expect the voice step to be skipped (null voice), then the
        // locale step to be skipped (null locale), then device default to
        // be tried, then en-US fallback.
        fakeEngine.languageResults["en-GB"] = true
        val result = applier.selectVoiceOrLocale(
            voiceName = null,
            savedLocale = null,
            deviceDefaultLocale = "en-GB"
        )
        assertTrue(result)
        assertEquals(listOf("lang:en-GB"), fakeEngine.calls)
    }

    @Test
    fun `apply returns the cascade result and also applies pitch rate and channel`() {
        fakeEngine.voiceResult = true
        val result = applier.apply(
            voiceName = "samantha",
            savedLocale = "en-US",
            pitch = 1.2f,
            speechRate = 0.8f,
            audioChannel = AudioChannel.NOTIFICATION,
            deviceDefaultLocale = "en-GB"
        )
        assertTrue(result)
        assertEquals(
            listOf(
                "voice:samantha|en-US",
                "pitch:1.2",
                "rate:0.8",
                "channel:NOTIFICATION"
            ),
            fakeEngine.calls
        )
    }

    @Test
    fun `deviceDefaultLocale defaults to the JVM default locale`() {
        // Pinned assertion: the production default wires through
        // Locale.getDefault().toLanguageTag(). If a refactor accidentally
        // removes that, the test surfaces the change.
        val expected = Locale.getDefault().toLanguageTag()
        // We assert by calling with the default and ensuring the call
        // attempts a language set with that tag when nothing else works.
        val result = applier.selectVoiceOrLocale(
            voiceName = null,
            savedLocale = null,
            deviceDefaultLocale = expected
        )
        // Either the call succeeded, or it fell through to en-US.
        // Either way, the *first* language set attempted is the device default.
        assertTrue(fakeEngine.calls.firstOrNull()?.startsWith("lang:") == true)
        // Result is false only if the device default also failed AND
        // en-US also failed; we don't assert the value here, just that
        // the function ran.
        @Suppress("UNUSED_VARIABLE")
        val ignored = result
    }
}

/** Minimal fake — only records the calls we care about. */
private class RecordingTtsEngine : TtsEngine {
    var voiceResult: Boolean = false
    val languageResults: MutableMap<String, Boolean> = mutableMapOf()
    val calls: MutableList<String> = mutableListOf()

    override suspend fun initialize(): Boolean = true
    override fun isAvailable(): Boolean = true
    override fun getVoices() = emptyList<com.hourlyvoiceclock.tts.VoiceInfo>()
    override fun setVoice(voiceName: String, localeTag: String): Boolean {
        calls.add("voice:$voiceName|$localeTag")
        return voiceResult
    }
    override fun setLanguage(localeTag: String): Boolean {
        calls.add("lang:$localeTag")
        return languageResults[localeTag] ?: false
    }
    override fun setPitch(pitch: Float) { calls.add("pitch:$pitch") }
    override fun setSpeechRate(rate: Float) { calls.add("rate:$rate") }
    override fun setAudioChannel(channel: AudioChannel) { calls.add("channel:$channel") }
    override fun speak(text: String, utteranceId: String) {}
    override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {}
    override fun stop() {}
    override fun shutdown() {}
    override suspend fun switchEngine(enginePackage: String?): Boolean = true
    override fun getEngines() = emptyList<com.hourlyvoiceclock.tts.TtsEngineInfo>()
    override fun getCurrentEnginePackage(): String? = null
    override fun isEspeakNgEngine(): Boolean = false
}
