package com.hourlyvoiceclock.tts

import com.hourlyvoiceclock.data.AudioChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TtsVoiceRepositoryTest {

    private class FakeTtsEngine : TtsEngine {
        var initialized = false
        var engineIsAvailable = false
        var engineVoices = listOf<VoiceInfo>()

        override suspend fun initialize(): Boolean {
            initialized = true
            engineIsAvailable = true
            return true
        }

        override fun isAvailable(): Boolean = engineIsAvailable

        override fun getVoices(): List<VoiceInfo> = engineVoices

        override fun setVoice(voiceName: String, localeTag: String): Boolean = true
        override fun setLanguage(localeTag: String): Boolean = true
        override fun setPitch(pitch: Float) {}
        override fun setSpeechRate(rate: Float) {}
        override fun setAudioChannel(channel: AudioChannel) {}
        override fun speak(text: String, utteranceId: String) {}
        override fun stop() {}
        override fun shutdown() {}
        override suspend fun switchEngine(enginePackage: String?): Boolean = true
        override fun getEngines(): List<TtsEngineInfo> = emptyList()
    }

    private lateinit var fakeEngine: FakeTtsEngine
    private lateinit var repository: TtsVoiceRepository

    @Before
    fun setup() {
        fakeEngine = FakeTtsEngine()
        repository = TtsVoiceRepository(fakeEngine)
    }

    @Test
    fun getNormalVoicesGroupedByLocale_filtersSpecialAndGroups() {
        // Arrange
        val voice1 = createVoice(name = "voice1", localeDisplayName = "English", isSpecial = false)
        val voice2 = createVoice(name = "voice2", localeDisplayName = "English", isSpecial = false)
        val voice3 = createVoice(name = "voice3", localeDisplayName = "French", isSpecial = false)
        val specialVoice = createVoice(name = "special", localeDisplayName = "English", isSpecial = true)

        fakeEngine.engineVoices = listOf(voice1, voice2, voice3, specialVoice)

        // Act
        val result = repository.getNormalVoicesGroupedByLocale()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.containsKey("English"))
        assertTrue(result.containsKey("French"))

        val englishVoices = result["English"]!!
        assertEquals(2, englishVoices.size)
        assertTrue(englishVoices.contains(voice1))
        assertTrue(englishVoices.contains(voice2))
        // Verify special voice is NOT in the list
        assertTrue(englishVoices.none { it.name == "special" })

        val frenchVoices = result["French"]!!
        assertEquals(1, frenchVoices.size)
        assertTrue(frenchVoices.contains(voice3))
    }

    @Test
    fun getNormalVoicesGroupedByLocale_emptyVoices() {
        fakeEngine.engineVoices = emptyList()

        val result = repository.getNormalVoicesGroupedByLocale()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getNormalVoicesGroupedByLocale_onlySpecialVoices() {
         val specialVoice1 = createVoice(name = "special1", localeDisplayName = "English", isSpecial = true)
         val specialVoice2 = createVoice(name = "special2", localeDisplayName = "French", isSpecial = true)

         fakeEngine.engineVoices = listOf(specialVoice1, specialVoice2)

         val result = repository.getNormalVoicesGroupedByLocale()

         assertTrue(result.isEmpty())
    }

    private fun createVoice(
        name: String,
        localeDisplayName: String,
        isSpecial: Boolean,
        localeTag: String = "en-US"
    ) = VoiceInfo(
        name = name,
        localeDisplayName = localeDisplayName,
        localeTag = localeTag,
        quality = 1,
        latency = 1,
        requiresNetwork = false,
        genderLabel = "Female",
        description = "Test Voice",
        isSpecial = isSpecial
    )
}
