package com.hourlyvoiceclock

import com.hourlyvoiceclock.data.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `cleanVersion strips leading v and whitespace`() {
        assertEquals("0.1", UpdateChecker.cleanVersion("v0.1"))
        assertEquals("0.1", UpdateChecker.cleanVersion("  v0.1  "))
        assertEquals("1.0.3", UpdateChecker.cleanVersion("V1.0.3"))
        assertEquals("0.2", UpdateChecker.cleanVersion("0.2"))
    }

    @Test
    fun `isNewerVersion returns true when latest version is higher`() {
        // Simple major.minor comparison
        assertTrue(UpdateChecker.isNewerVersion("0.1", "0.2"))
        assertTrue(UpdateChecker.isNewerVersion("0.1", "1.0"))

        // Patch level comparison
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.0", "1.0.1"))
    }

    @Test
    fun `isNewerVersion returns false when latest version is same or older`() {
        // Matching versions
        assertFalse(UpdateChecker.isNewerVersion("0.1", "0.1"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0"))

        // Older version on remote
        assertFalse(UpdateChecker.isNewerVersion("0.2", "0.1"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.1", "1.0"))
    }

    @Test
    fun `isNewerVersion handles empty or blank version values safely`() {
        assertFalse(UpdateChecker.isNewerVersion("", "0.1"))
        assertFalse(UpdateChecker.isNewerVersion("0.1", ""))
        assertFalse(UpdateChecker.isNewerVersion("   ", "   "))
    }

    @Test
    fun `isNewerVersion handles non-numeric parts gracefully`() {
        // Falling back to 0 for invalid sections
        assertFalse(UpdateChecker.isNewerVersion("0.1a", "0.1"))
        assertTrue(UpdateChecker.isNewerVersion("0.1", "0.2-beta"))
    }
}
