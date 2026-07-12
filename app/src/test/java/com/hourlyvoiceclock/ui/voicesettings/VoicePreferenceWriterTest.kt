package com.hourlyvoiceclock.ui.voicesettings

import com.hourlyvoiceclock.R
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

@RunWith(RobolectricTestRunner::class)
class VoicePreferenceWriterTest {

    private val context get() = RuntimeEnvironment.getApplication()
    private val engine = FakeTtsEngine()
    private val repo = SettingsRepository(context)
    private val writer = VoicePreferenceWriter(engine, repo)

    @After
    fun tearDown() = runBlocking {
        repo.update {
            it.copy(
                selectedVoiceName = null,
                selectedLocale = null,
                selectedVoicePresetId = null,
                selectedLocalModelId = null,
                pitch = 1.0f,
                speechRate = 1.0f
            )
        }
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
    fun `setVoice clears selectedLocalModelId`() = runBlocking {
        repo.update { it.copy(selectedLocalModelId = "en_US-lessac-medium") }

        writer.setVoice("samantha", "en-US")

        assertNull(repo.settings.first().selectedLocalModelId)
    }

    @Test
    fun `setPitch writes to both engine and repository`() = runBlocking {
        writer.setPitch(1.4f)

        assertEquals(1.4f, engine.lastPitch, 0.0001f)
        assertEquals(1.4f, repo.settings.first().pitch, 0.0001f)
    }

    @Test
    fun `setPitch clears selectedVoicePresetId`() = runBlocking {
        repo.update { it.copy(selectedVoicePresetId = "preset_robot") }

        writer.setPitch(1.2f)

        assertNull(repo.settings.first().selectedVoicePresetId)
    }

    @Test
    fun `setSpeechRate writes to both engine and repository`() = runBlocking {
        writer.setSpeechRate(0.6f)

        assertEquals(0.6f, engine.lastSpeechRate, 0.0001f)
        assertEquals(0.6f, repo.settings.first().speechRate, 0.0001f)
    }

    @Test
    fun `setSpeechRate clears selectedVoicePresetId`() = runBlocking {
        repo.update { it.copy(selectedVoicePresetId = "preset_chipmunk") }

        writer.setSpeechRate(1.1f)

        assertNull(repo.settings.first().selectedVoicePresetId)
    }

    @Test
    fun `applyPreset writes all five fields to both engine and repository`() = runBlocking {
        val preset = SpecialVoicePreset(
            id = "preset_robot",
            nameRes = R.string.preset_robot_name,
            descriptionRes = R.string.preset_robot_desc,
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

        assertEquals(voice.name, engine.lastVoiceName)
        assertEquals(voice.localeTag, engine.lastLocaleTag)
        assertEquals(preset.pitch, engine.lastPitch, 0.0001f)
        assertEquals(preset.speechRate, engine.lastSpeechRate, 0.0001f)

        val stored = repo.settings.first()
        assertEquals(voice.name, stored.selectedVoiceName)
        assertEquals(voice.localeTag, stored.selectedLocale)
        assertEquals(preset.id, stored.selectedVoicePresetId)
        assertEquals(preset.pitch, stored.pitch, 0.0001f)
        assertEquals(preset.speechRate, stored.speechRate, 0.0001f)
    }

    @Test
    fun `applyPreset clears selectedLocalModelId`() = runBlocking {
        repo.update { it.copy(selectedLocalModelId = "en_US-amy-medium") }
        val preset = SPECIAL_VOICE_PRESETS.first { it.id == "preset_robot" }
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

        assertNull(repo.settings.first().selectedLocalModelId)
        assertEquals("preset_robot", repo.settings.first().selectedVoicePresetId)
    }

    @Test
    fun `engine and repository are written before the suspend returns`() = runBlocking {
        writer.setVoice("ava", "en-GB")

        assertTrue("Engine write missed", engine.lastVoiceName == "ava")
        assertTrue("Repository write missed", repo.settings.first().selectedVoiceName == "ava")
    }
}
