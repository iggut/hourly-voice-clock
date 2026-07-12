package com.hourlyvoiceclock.tts.local

import androidx.test.core.app.ApplicationProvider
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceModelRegistryTest {

    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun getVoiceById_existingId_returnsVoice() {
        val voice = VoiceModelRegistry.getVoiceById("piper_en_us_amy_medium")

        assertNotNull(voice)
        assertEquals("piper_en_us_amy_medium", voice?.id)
        assertEquals(VoiceCategory.STANDARD, voice?.category)
        assertEquals(VoiceSourceKind.OFFICIAL, voice?.sourceKind)
    }

    @Test
    fun getVoiceById_nonExistingId_returnsNull() {
        val voice = VoiceModelRegistry.getVoiceById("non_existing_invalid_id_123")

        assertNull(voice)
    }

    @Test
    fun getVoicesByCategory_returnsFilteredList() {
        val characterVoices = VoiceModelRegistry.getVoicesByCategory(VoiceCategory.CHARACTER)
        val standardVoices = VoiceModelRegistry.getVoicesByCategory(VoiceCategory.STANDARD)

        assertTrue("Character voices list should not be empty", characterVoices.isNotEmpty())
        assertTrue(
            "All voices in character list should be of category CHARACTER",
            characterVoices.all { it.category == VoiceCategory.CHARACTER }
        )

        assertTrue("Standard voices list should not be empty", standardVoices.isNotEmpty())
        assertTrue(
            "All voices in standard list should be of category STANDARD",
            standardVoices.all { it.category == VoiceCategory.STANDARD }
        )
    }

    @Test
    fun formatSize_formatsCorrectly() {
        // Below 1 MB uses KB with whole-number formatting (%.0f).
        assertEquals("512 KB", VoiceModelRegistry.formatSize(context, 512 * 1024))
        assertEquals("1.0 MB", VoiceModelRegistry.formatSize(context, 1024 * 1024))
        assertEquals("1.5 MB", VoiceModelRegistry.formatSize(context, (1.5 * 1024 * 1024).toLong()))
        assertEquals("100.0 MB", VoiceModelRegistry.formatSize(context, 100 * 1024 * 1024))
    }

    @Test
    fun catalog_hasUniqueIds_andStringResources() {
        val ids = VoiceModelRegistry.availableVoices.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        VoiceModelRegistry.availableVoices.forEach { model ->
            assertTrue(model.displayNameRes != 0)
            assertTrue(model.descriptionRes != 0)
            assertTrue(context.getString(model.displayNameRes).isNotBlank())
            assertTrue(context.getString(model.descriptionRes).isNotBlank())
        }
    }

    @Test
    fun communityVoices_areMarkedPersonalTesting() {
        VoiceModelRegistry.availableVoices
            .filter { it.sourceKind == VoiceSourceKind.COMMUNITY }
            .forEach { assertTrue(it.id, it.personalTestingOnly) }
    }

    @Test
    fun officialAdditions_arePresent() {
        listOf(
            "piper_en_us_hfc_female_medium",
            "piper_en_us_joe_medium",
            "piper_en_gb_jenny_dioco_medium",
            "piper_en_us_lessac_high",
            "jarvis_medium",
            "hal_9000_no_denoise_dividebysandwich"
        ).forEach { id ->
            assertNotNull(id, VoiceModelRegistry.getVoiceById(id))
        }
    }

    @Test
    fun arcticAndKusal_remainOfficial() {
        val arctic = VoiceModelRegistry.getVoiceById("piper_en_us_arctic_medium")
        val kusal = VoiceModelRegistry.getVoiceById("piper_en_us_kusal_medium")
        assertEquals(VoiceSourceKind.OFFICIAL, arctic?.sourceKind)
        assertFalse(arctic!!.personalTestingOnly)
        assertEquals(VoiceSourceKind.OFFICIAL, kusal?.sourceKind)
        assertFalse(kusal!!.personalTestingOnly)
    }
}
