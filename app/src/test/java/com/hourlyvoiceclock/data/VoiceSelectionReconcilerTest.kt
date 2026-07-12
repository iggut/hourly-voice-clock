package com.hourlyvoiceclock.data

import com.hourlyvoiceclock.tts.VoiceInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceSelectionReconcilerTest {

    @Test
    fun `clears selectedLocalModelId when model is not on disk`() = runBlocking {
        var stored = AppSettings(selectedLocalModelId = "known-model")
        val reconciler = VoiceSelectionReconciler(
            loadSettings = { stored },
            updateSettings = { transform -> stored = transform(stored) },
            selectEnginePackage = { null },
            initializeEngine = { },
            listSystemVoices = { emptyList() },
            isKnownLocalModel = { it == "known-model" },
            isLocalModelDownloaded = { false }
        )

        reconciler.reconcile()

        assertNull(stored.selectedLocalModelId)
    }

    @Test
    fun `clears stale system voice name`() = runBlocking {
        var stored = AppSettings(
            selectedVoiceName = "gone-voice",
            selectedLocale = "en-US",
            selectedVoicePresetId = "preset_robot"
        )
        val reconciler = VoiceSelectionReconciler(
            loadSettings = { stored },
            updateSettings = { transform -> stored = transform(stored) },
            selectEnginePackage = { null },
            initializeEngine = { },
            listSystemVoices = {
                listOf(
                    VoiceInfo(
                        name = "still-here",
                        localeDisplayName = "United States",
                        localeTag = "en-US",
                        quality = 400,
                        latency = 200,
                        requiresNetwork = false,
                        genderLabel = "Female",
                        description = "Still here",
                        isSpecial = false
                    )
                )
            },
            isKnownLocalModel = { false },
            isLocalModelDownloaded = { false }
        )

        reconciler.reconcile()

        assertNull(stored.selectedVoiceName)
        assertNull(stored.selectedLocale)
        assertNull(stored.selectedVoicePresetId)
    }

    @Test
    fun `keeps local model when present on disk`() = runBlocking {
        var stored = AppSettings(selectedLocalModelId = "known-model")
        val reconciler = VoiceSelectionReconciler(
            loadSettings = { stored },
            updateSettings = { transform -> stored = transform(stored) },
            selectEnginePackage = { null },
            initializeEngine = { },
            listSystemVoices = { emptyList() },
            isKnownLocalModel = { it == "known-model" },
            isLocalModelDownloaded = { it == "known-model" }
        )

        reconciler.reconcile()

        assertEquals("known-model", stored.selectedLocalModelId)
    }
}
