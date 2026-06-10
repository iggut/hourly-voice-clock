package com.hourlyvoiceclock.tts

import com.hourlyvoiceclock.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Pins the policy that the saved TTS engine package is honoured only if
 * the package is still installed, and is cleared from the repository when
 * it is not. The previous version of this logic was buried inside
 * AndroidTtsEngine.initialize() and bypassed the DI graph by
 * instantiating a second SettingsRepository inline.
 */
@RunWith(RobolectricTestRunner::class)
class TtsEngineSelectorTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() = runBlocking {
        // Reset the persisted saved engine so other tests start clean.
        SettingsRepository(context).setSelectedTtsEnginePackage(null)
    }

    @Test
    fun `returns null when no saved package`() = runBlocking {
        val selector = TtsEngineSelector(
            settings = SettingsRepository(context),
            packageProbe = FakeProbe(installed = emptySet())
        )
        assertNull(selector.select())
    }

    @Test
    fun `returns saved package when probe says it is installed`() = runBlocking {
        val repo = SettingsRepository(context)
        repo.setSelectedTtsEnginePackage("com.example.tts")

        val selector = TtsEngineSelector(
            settings = repo,
            packageProbe = FakeProbe(installed = setOf("com.example.tts"))
        )
        assertEquals("com.example.tts", selector.select())
    }

    @Test
    fun `clears saved package and returns null when probe says it is not installed`() = runBlocking {
        val repo = SettingsRepository(context)
        repo.setSelectedTtsEnginePackage("com.uninstalled.tts")

        val selector = TtsEngineSelector(
            settings = repo,
            packageProbe = FakeProbe(installed = emptySet())
        )
        assertNull(selector.select())

        // The repository must have been told to forget the stale value,
        // so the next read returns null.
        assertNull(repo.settings.first().selectedTtsEnginePackage)
    }

    @Test
    fun `blank saved package is treated as no saved package`() = runBlocking {
        val repo = SettingsRepository(context)
        repo.setSelectedTtsEnginePackage("")

        val selector = TtsEngineSelector(
            settings = repo,
            packageProbe = FakeProbe(installed = setOf("com.example.tts"))
        )
        assertNull(selector.select())
    }
}

private class FakeProbe(private val installed: Set<String>) : TtsPackageProbe {
    override fun isInstalled(packageName: String): Boolean = packageName in installed
}
