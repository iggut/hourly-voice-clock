package com.hourlyvoiceclock.tts.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocalVoiceDownloadQueueTest {

    private val amy = VoiceModelRegistry.getVoiceById("piper_en_us_amy_medium")!!
    private val lessac = VoiceModelRegistry.getVoiceById("piper_en_us_lessac_medium")!!
    private val alba = VoiceModelRegistry.getVoiceById("piper_en_gb_alba_medium")!!

    @Test
    fun `two downloads can run concurrently and third waits`() = runTest {
        val active = AtomicInteger(0)
        val maxSeen = AtomicInteger(0)
        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())

        val queue = LocalVoiceDownloadQueue(
            scope = this,
            downloader = downloader,
            maxConcurrency = 2,
            download = { model, onProgress ->
                val now = active.incrementAndGet()
                maxSeen.updateAndGet { maxOf(it, now) }
                try {
                    delay(50)
                    onProgress(1f)
                    Result.success(File("/tmp/${model.id}"))
                } finally {
                    active.decrementAndGet()
                }
            },
            isDownloaded = { false }
        )

        assertTrue(queue.enqueue(amy))
        assertTrue(queue.enqueue(lessac))
        assertTrue(queue.enqueue(alba))

        advanceUntilIdle()

        assertEquals("Expected concurrent peak of 2", 2, maxSeen.get())
        assertTrue(queue.progressByModelId.value.isEmpty())
    }

    @Test
    fun `duplicate id does not start twice`() = runTest {
        val starts = AtomicInteger(0)
        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val queue = LocalVoiceDownloadQueue(
            scope = this,
            downloader = downloader,
            maxConcurrency = 2,
            download = { model, _ ->
                starts.incrementAndGet()
                delay(100)
                Result.success(File("/tmp/${model.id}"))
            },
            isDownloaded = { false }
        )

        assertTrue(queue.enqueue(amy))
        assertFalse(queue.enqueue(amy))

        advanceUntilIdle()
        assertEquals(1, starts.get())
    }

    @Test
    fun `cancel clears progress and leaves model not installed`() = runTest {
        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val queue = LocalVoiceDownloadQueue(
            scope = this,
            downloader = downloader,
            maxConcurrency = 2,
            download = { _, onProgress ->
                onProgress(0.25f)
                delay(10_000)
                Result.success(File("/tmp/should-not-finish"))
            },
            isDownloaded = { false }
        )

        assertTrue(queue.enqueue(amy))
        // Run until the download sets progress, but not through the long delay.
        testScheduler.runCurrent()
        assertTrue(queue.progressByModelId.value.containsKey(amy.id))

        queue.cancel(amy.id)

        assertFalse(queue.progressByModelId.value.containsKey(amy.id))
        assertFalse(downloader.isModelDownloaded(amy))
    }
}
