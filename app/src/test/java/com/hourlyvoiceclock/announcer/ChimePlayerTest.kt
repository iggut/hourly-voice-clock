package com.hourlyvoiceclock.announcer

import android.content.Context
import com.hourlyvoiceclock.data.ChimeSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
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
}
