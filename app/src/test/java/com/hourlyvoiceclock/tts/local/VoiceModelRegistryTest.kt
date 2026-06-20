package com.hourlyvoiceclock.tts.local

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceModelRegistryTest {

    @Test
    fun formatSize_zeroBytes_returnsZeroKb() {
        val sizeBytes = 0L
        val formatted = VoiceModelRegistry.formatSize(sizeBytes)
        assertEquals("0 KB", formatted)
    }

    @Test
    fun formatSize_lessThanOneMb_returnsKb() {
        val sizeBytes = 512L * 1024L // 512 KB
        val formatted = VoiceModelRegistry.formatSize(sizeBytes)
        assertEquals("512 KB", formatted)
    }

    @Test
    fun formatSize_exactlyOneMb_returnsMb() {
        val sizeBytes = 1024L * 1024L // 1 MB
        val formatted = VoiceModelRegistry.formatSize(sizeBytes)
        assertEquals("1.0 MB", formatted)
    }

    @Test
    fun formatSize_moreThanOneMb_returnsMb() {
        val sizeBytes = (1.5 * 1024 * 1024).toLong() // 1.5 MB
        val formatted = VoiceModelRegistry.formatSize(sizeBytes)
        assertEquals("1.5 MB", formatted)
    }

    @Test
    fun formatSize_roundsKbCorrectly() {
        // 512.6 KB
        val sizeBytes = (512.6 * 1024).toLong()
        val formatted = VoiceModelRegistry.formatSize(sizeBytes)
        assertEquals("513 KB", formatted)
    }
}
