package com.hourlyvoiceclock.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the voice-selection fallback cascade inside [AndroidTtsEngine].
 *
 * The cascade is exposed as an internal file-level function so it can be
 * unit-tested without constructing a real [android.speech.tts.TextToSpeech].
 */
class AndroidTtsEngineTest {

    @Test
    fun `saved voice is tried first`() {
        val engine = RecordingEngine(voiceResult = true)

        val result = selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB",
            setVoice = engine::setVoice,
            setLanguage = engine::setLanguage
        )

        assertTrue(result)
        assertEquals(listOf("voice:samantha|en-US"), engine.calls)
    }

    @Test
    fun `falls back to saved locale when voice fails`() {
        val engine = RecordingEngine(languageResults = mapOf("en-US" to true))

        val result = selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB",
            setVoice = engine::setVoice,
            setLanguage = engine::setLanguage
        )

        assertTrue(result)
        assertEquals(listOf("voice:samantha|en-US", "lang:en-US"), engine.calls)
    }

    @Test
    fun `falls back to device default when voice and saved locale both fail`() {
        val engine = RecordingEngine(languageResults = mapOf("en-GB" to true))

        val result = selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB",
            setVoice = engine::setVoice,
            setLanguage = engine::setLanguage
        )

        assertTrue(result)
        assertEquals(
            listOf("voice:samantha|en-US", "lang:en-US", "lang:en-GB"),
            engine.calls
        )
    }

    @Test
    fun `falls back to en-US hard fallback when nothing else works`() {
        val engine = RecordingEngine()

        val result = selectVoiceOrLocale(
            voiceName = "samantha",
            savedLocale = "fr-FR",
            deviceDefaultLocale = "en-GB",
            setVoice = engine::setVoice,
            setLanguage = engine::setLanguage
        )

        assertFalse(result)
        assertEquals(
            listOf("voice:samantha|fr-FR", "lang:fr-FR", "lang:en-GB", "lang:en-US"),
            engine.calls
        )
    }

    @Test
    fun `blank voice name is skipped and cascade continues from locale`() {
        val engine = RecordingEngine(languageResults = mapOf("en-US" to true))

        val result = selectVoiceOrLocale(
            voiceName = "",
            savedLocale = "en-US",
            deviceDefaultLocale = "en-GB",
            setVoice = engine::setVoice,
            setLanguage = engine::setLanguage
        )

        assertTrue(result)
        assertEquals(listOf("lang:en-US"), engine.calls)
    }

    @Test
    fun `null saved locale is skipped at the locale step`() {
        val engine = RecordingEngine(languageResults = mapOf("en-GB" to true))

        val result = selectVoiceOrLocale(
            voiceName = null,
            savedLocale = null,
            deviceDefaultLocale = "en-GB",
            setVoice = engine::setVoice,
            setLanguage = engine::setLanguage
        )

        assertTrue(result)
        assertEquals(listOf("lang:en-GB"), engine.calls)
    }

    private class RecordingEngine(
        private val voiceResult: Boolean = false,
        private val languageResults: Map<String, Boolean> = emptyMap()
    ) {
        val calls = mutableListOf<String>()

        fun setVoice(voiceName: String, localeTag: String): Boolean {
            calls.add("voice:$voiceName|$localeTag")
            return voiceResult
        }

        fun setLanguage(localeTag: String): Boolean {
            calls.add("lang:$localeTag")
            return languageResults[localeTag] ?: false
        }
    }
}
