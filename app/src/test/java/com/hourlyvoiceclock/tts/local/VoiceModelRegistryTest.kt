package com.hourlyvoiceclock.tts.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModelRegistryTest {

    @Test
    fun getVoiceById_existingId_returnsVoice() {
        // Arrange & Act
        val voice = VoiceModelRegistry.getVoiceById("piper_en_us_amy_medium")

        // Assert
        assertNotNull(voice)
        assertEquals("piper_en_us_amy_medium", voice?.id)
        assertEquals(VoiceCategory.STANDARD, voice?.category)
    }

    @Test
    fun getVoiceById_nonExistingId_returnsNull() {
        // Arrange & Act
        val voice = VoiceModelRegistry.getVoiceById("non_existing_invalid_id_123")

        // Assert
        assertNull(voice)
    }

    @Test
    fun getVoicesByCategory_returnsFilteredList() {
        // Arrange & Act
        val characterVoices = VoiceModelRegistry.getVoicesByCategory(VoiceCategory.CHARACTER)
        val standardVoices = VoiceModelRegistry.getVoicesByCategory(VoiceCategory.STANDARD)

        // Assert
        assertTrue("Character voices list should not be empty", characterVoices.isNotEmpty())
        assertTrue("All voices in character list should be of category CHARACTER",
            characterVoices.all { it.category == VoiceCategory.CHARACTER })

        assertTrue("Standard voices list should not be empty", standardVoices.isNotEmpty())
        assertTrue("All voices in standard list should be of category STANDARD",
            standardVoices.all { it.category == VoiceCategory.STANDARD })
    }

    @Test
    fun formatSize_formatsCorrectly() {
        // KB range
        assertEquals("1024 KB", VoiceModelRegistry.formatSize(1024 * 1024 - 1))

        // MB range (exact)
        assertEquals("1.0 MB", VoiceModelRegistry.formatSize(1024 * 1024))

        // MB range (fractional)
        assertEquals("1.5 MB", VoiceModelRegistry.formatSize((1.5 * 1024 * 1024).toLong()))

        // Large MB range
        assertEquals("100.0 MB", VoiceModelRegistry.formatSize(100 * 1024 * 1024))
    }
}
