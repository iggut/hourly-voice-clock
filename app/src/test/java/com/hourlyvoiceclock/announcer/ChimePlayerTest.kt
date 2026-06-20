package com.hourlyvoiceclock.announcer

import android.content.Context
import com.hourlyvoiceclock.data.ChimeSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChimePlayerTest {

    private val player = ChimePlayer(mock(Context::class.java))

    @Test
    fun `NONE maps to 0`() {
        assertEquals(0, player.resourceIdFor(ChimeSound.NONE))
    }

    @Test
    fun `every non-NONE sound maps to a non-zero resource id`() {
        ChimeSound.values()
            .filter { it != ChimeSound.NONE }
            .forEach { sound ->
                assertNotEquals("Expected a raw resource for $sound", 0, player.resourceIdFor(sound))
            }
    }

    @Test
    fun `each non-NONE sound maps to a distinct resource id`() {
        val sounds = ChimeSound.values().filter { it != ChimeSound.NONE }
        val ids = sounds.map { player.resourceIdFor(it) }
        assertEquals("Duplicate resource mapping", sounds.size, ids.toSet().size)
    }

    @Test
    fun `play calls onComplete when MediaPlayer creation fails with exception`() {
        val mockContext = mock(Context::class.java)
        // Force MediaPlayer.create() to throw an exception by providing a mock Context
        // whose resources throw. MediaPlayer uses context.resources.openRawResourceFd() internally.
        `when`(mockContext.resources).thenThrow(RuntimeException("Simulated context exception"))

        val failingPlayer = ChimePlayer(mockContext)
        var completed = false
        failingPlayer.play(ChimeSound.CLASSIC_CHIME) {
            completed = true
        }

        assertTrue("onComplete should be called when playback fails", completed)
    }
}
