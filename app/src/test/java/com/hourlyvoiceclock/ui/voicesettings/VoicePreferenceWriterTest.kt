package com.hourlyvoiceclock.ui.voicesettings

import com.hourlyvoiceclock.FakeTtsEngine
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.tts.VoiceInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Pins the dual-write behaviour: every preference setter must update
 * both the live TtsEngine AND the SettingsRepository. The previous
 * code repeated this fan-out 5-6 times verbatim in
 * VoiceSettingsViewModel, and a regression in one of the call sites
 * would silently leave engine and repository out of sync.
 */
@RunWith(RobolectricTestRunner::class)
class VoicePreferenceWriterTest {

    private val context get() = RuntimeEnvironment.getApplication()
    private val engine = FakeTtsEngine()
    private val repo = SettingsRepository(context)
    private val writer = VoicePreferenceWriter(engine, repo)

    @After
    fun tearDown() = runBlocking {
        // Reset every field we may have written, so other tests start clean.
        repo.setSelectedVoice(null, null)
        repo.setSelectedVoicePreset(null)
        repo.setPitch(1.0f)
        repo.setSpeechRate(1.0f)
    }

    @Test
    fun `setVoice writes to both engine and repository`() = runBlocking {
        writer.setVoice("samantha", "en-US")

        assertEquals("samantha", engine.lastVoiceName)
        assertEquals("en-US", engine.lastLocaleTag)
        val stored = repo.settings.first()
        assertEquals("samantha", stored.selectedVoiceName)
        assertEquals("en-US", stored.selectedLocale)
        assertNull("Voice preset id should be cleared when selecting a raw voice", stored.selectedVoicePresetId)
    }

    @Test
    fun `setPitch writes to both engine and repository`() = runBlocking {
        writer.setPitch(1.4f)

        assertEquals(1.4f, engine.lastPitch, 0.0001f)
        assertEquals(1.4f, repo.settings.first().pitch, 0.0001f)
    }

    @Test
    fun `setSpeechRate writes to both engine and repository`() = runBlocking {
        writer.setSpeechRate(0.6f)

        assertEquals(0.6f, engine.lastSpeechRate, 0.0001f)
        assertEquals(0.6f, repo.settings.first().speechRate, 0.0001f)
    }

    @Test
    fun `applyPreset writes all five fields to both engine and repository`() = runBlocking {
        val preset = SpecialVoicePreset(
            id = "preset_robot",
            displayName = "Robot",
            pitch = 0.5f,
            speechRate = 1.1f,
            preferredGender = "Male",
            preferredLocalePrefixes = listOf("en"),
            voiceNameHints = emptyList(),
            prefersNetworkVoice = null
        )
        val voice = VoiceInfo(
            name = "en-us-x-sfg#male_1-local",
            localeDisplayName = "United States",
            localeTag = "en-US",
            quality = 400,
            latency = 200,
            requiresNetwork = false,
            genderLabel = "Male",
            description = "US Voice 1",
            isSpecial = false
        )

        writer.applyPreset(preset, voice)

        // Engine state
        assertEquals(voice.name, engine.lastVoiceName)
        assertEquals(voice.localeTag, engine.lastLocaleTag)
        assertEquals(preset.pitch, engine.lastPitch, 0.0001f)
        assertEquals(preset.speechRate, engine.lastSpeechRate, 0.0001f)

        // Repository state — single atomic update
        val stored = repo.settings.first()
        assertEquals(voice.name, stored.selectedVoiceName)
        assertEquals(voice.localeTag, stored.selectedLocale)
        assertEquals(preset.id, stored.selectedVoicePresetId)
        assertEquals(preset.pitch, stored.pitch, 0.0001f)
        assertEquals(preset.speechRate, stored.speechRate, 0.0001f)
    }

    @Test
    fun `engine and repository are written before the suspend returns`() = runBlocking {
        // If the writer did the engine write in one launch and the
        // repository write in another, an early observer could see a
        // half-applied state. Confirm both writes complete before the
        // suspend function returns.
        writer.setVoice("ava", "en-GB")

        // At this point in the test (synchronous after the suspend
        // returns), both writes must have happened.
        assertTrue("Engine write missed", engine.lastVoiceName == "ava")
        assertTrue("Repository write missed", repo.settings.first().selectedVoiceName == "ava")
    }
}
