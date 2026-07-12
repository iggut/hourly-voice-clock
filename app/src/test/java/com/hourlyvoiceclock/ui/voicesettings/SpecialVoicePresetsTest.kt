package com.hourlyvoiceclock.ui.voicesettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialVoicePresetsTest {

    @Test
    fun `special presets have unique ids`() {
        val ids = SPECIAL_VOICE_PRESETS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `espeak presets have unique ids`() {
        val ids = ESpeakNgVoiceVariants.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all preset ids are unique across special and espeak lists`() {
        val ids = (SPECIAL_VOICE_PRESETS + ESpeakNgVoiceVariants).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `includes the six new special presets`() {
        val ids = SPECIAL_VOICE_PRESETS.map { it.id }.toSet()
        listOf(
            "preset_whisper",
            "preset_news",
            "preset_auctioneer",
            "preset_drill",
            "preset_storyteller",
            "preset_sparkle"
        ).forEach { assertTrue("$it missing", it in ids) }
    }

    @Test
    fun `every special preset has string resources and a tag`() {
        SPECIAL_VOICE_PRESETS.forEach { preset ->
            assertTrue("${preset.id} missing nameRes", preset.nameRes != 0)
            assertTrue("${preset.id} missing descriptionRes", preset.descriptionRes != 0)
            assertTrue("${preset.id} should not use ESPEAK tag", preset.tag != SpecialVoiceTag.ESPEAK)
        }
        ESpeakNgVoiceVariants.forEach { preset ->
            assertTrue("${preset.id} missing nameRes", preset.nameRes != 0)
            assertTrue("${preset.id} missing descriptionRes", preset.descriptionRes != 0)
            assertEquals(SpecialVoiceTag.ESPEAK, preset.tag)
        }
    }

    @Test
    fun `preset gradients cover new and existing ids`() {
        val ids = (SPECIAL_VOICE_PRESETS + ESpeakNgVoiceVariants).map { it.id }
        ids.forEach { id ->
            presetGradientFor(id)
        }
    }
}
