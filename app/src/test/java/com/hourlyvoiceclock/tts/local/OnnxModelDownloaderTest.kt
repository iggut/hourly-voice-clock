package com.hourlyvoiceclock.tts.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for the existence check that drives the "already downloaded"
 * short-circuit and the on-disk layout produced by [OnnxModelDownloader].
 *
 * The real download path hits the network and is not exercised here;
 * the focus is the post-download state contract the rest of the app
 * depends on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OnnxModelDownloaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `registry URLs are all HTTPS and end with the onnx or json file`() {
        VoiceModelRegistry.availableVoices.forEach { model ->
            assertTrue(
                "Model ${model.id} onnxDownloadUrl must be https: ${model.onnxDownloadUrl}",
                model.onnxDownloadUrl.startsWith("https://")
            )
            assertTrue(
                "Model ${model.id} onnxJsonDownloadUrl must be https: ${model.onnxJsonDownloadUrl}",
                model.onnxJsonDownloadUrl.startsWith("https://")
            )
            assertTrue(
                "Model ${model.id} onnxFileName should end with .onnx: ${model.onnxFileName}",
                model.onnxFileName.endsWith(".onnx")
            )
            assertTrue(
                "Model ${model.id} onnxJsonFileName should end with .onnx.json: ${model.onnxJsonFileName}",
                model.onnxJsonFileName.endsWith(".onnx.json")
            )
        }
    }

    @Test
    fun `model has matching onnx and json file names`() {
        VoiceModelRegistry.availableVoices.forEach { model ->
            assertEquals(
                "File name and URL tail must match for ${model.id}",
                model.onnxDownloadUrl.substringAfterLast('/'),
                model.onnxFileName
            )
            assertEquals(
                "JSON file name and URL tail must match for ${model.id}",
                model.onnxJsonDownloadUrl.substringAfterLast('/'),
                model.onnxJsonFileName
            )
        }
    }

    @Test
    fun `isModelDownloaded returns false when files are missing`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()
        assertFalse(downloader.isModelDownloaded(model))
    }

    @Test
    fun `isModelDownloaded returns true when both files are present and non-empty`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()

        val modelDir = File(ctx.filesDir, "local_tts/models/${model.id}")
        modelDir.mkdirs()
        File(modelDir, model.onnxFileName).writeBytes(ByteArray(16) { 1 })
        File(modelDir, model.onnxJsonFileName).writeBytes(ByteArray(16) { 1 })

        assertTrue(downloader.isModelDownloaded(model))
    }

    @Test
    fun `isModelDownloaded returns false when only the onnx file is present`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()

        val modelDir = File(ctx.filesDir, "local_tts/models/${model.id}")
        modelDir.mkdirs()
        File(modelDir, model.onnxFileName).writeBytes(ByteArray(16) { 1 })
        // JSON file intentionally missing.

        assertFalse(downloader.isModelDownloaded(model))
    }

    @Test
    fun `isModelDownloaded returns false when the onnx file is empty`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()

        val modelDir = File(ctx.filesDir, "local_tts/models/${model.id}")
        modelDir.mkdirs()
        File(modelDir, model.onnxFileName).writeBytes(ByteArray(0))
        File(modelDir, model.onnxJsonFileName).writeBytes(ByteArray(16) { 1 })

        assertFalse(downloader.isModelDownloaded(model))
    }

    @Test
    fun `deleteModel removes the model directory`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()

        val modelDir = File(ctx.filesDir, "local_tts/models/${model.id}")
        modelDir.mkdirs()
        File(modelDir, model.onnxFileName).writeBytes(ByteArray(8) { 1 })

        assertTrue(downloader.deleteModel(model))
        assertFalse(downloader.isModelDownloaded(model))
    }

    @Test
    fun `getDownloadedModels returns only fully present voices`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val first = VoiceModelRegistry.availableVoices[0]
        val second = VoiceModelRegistry.availableVoices[1]

        // Fully install the first voice; leave the second half-installed.
        val firstDir = File(ctx.filesDir, "local_tts/models/${first.id}")
        firstDir.mkdirs()
        File(firstDir, first.onnxFileName).writeBytes(ByteArray(16) { 1 })
        File(firstDir, first.onnxJsonFileName).writeBytes(ByteArray(16) { 1 })

        val secondDir = File(ctx.filesDir, "local_tts/models/${second.id}")
        secondDir.mkdirs()
        File(secondDir, second.onnxFileName).writeBytes(ByteArray(16) { 1 })
        // JSON missing for second.

        val downloaded = downloader.getDownloadedModels()
        assertEquals(1, downloaded.size)
        assertEquals(first.id, downloaded[0].id)
    }

    @Test
    fun `DownloadException carries a message`() {
        val ex = DownloadException("HTTP 404 for x")
        assertNotNull(ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `DownloadException retains cause when provided`() {
        val cause = RuntimeException("socket closed")
        val ex = DownloadException("Network error: socket closed", cause)
        assertEquals(cause, ex.cause)
    }
}
