package com.hourlyvoiceclock

import com.hourlyvoiceclock.data.GitHubUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `cleanVersion strips leading v and whitespace`() {
        assertEquals("0.1.0", GitHubUpdateChecker.cleanVersion("v0.1.0"))
        assertEquals("0.1.0", GitHubUpdateChecker.cleanVersion("  v0.1.0  "))
        assertEquals("1.0.3", GitHubUpdateChecker.cleanVersion("V1.0.3"))
        assertEquals("0.2.0", GitHubUpdateChecker.cleanVersion("0.2.0"))
    }

    @Test
    fun `isNewerVersion returns true when latest version is higher`() {
        // Simple major.minor comparison
        assertTrue(GitHubUpdateChecker.isNewerVersion("0.1.0", "0.2.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("0.1.0", "1.0.0"))

        // Patch level comparison
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun `isNewerVersion returns false when latest version is same or older`() {
        // Matching versions
        assertFalse(GitHubUpdateChecker.isNewerVersion("0.1.0", "0.1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("1.0.0", "1.0.0"))

        // Older version on remote
        assertFalse(GitHubUpdateChecker.isNewerVersion("0.2.0", "0.1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun `isNewerVersion handles empty or blank version values safely`() {
        assertFalse(GitHubUpdateChecker.isNewerVersion("", "0.1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("0.1.0", ""))
        assertFalse(GitHubUpdateChecker.isNewerVersion("   ", "   "))
    }

    @Test
    fun `isNewerVersion handles non-numeric parts gracefully`() {
        // Falling back to 0 for invalid sections
        assertFalse(GitHubUpdateChecker.isNewerVersion("0.1.0-alpha", "0.1.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("0.1.0", "0.2.0-beta"))
    }
}
