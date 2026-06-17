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
    fun `isModelDownloaded returns false when current loader marker is missing`() {
        // Older installs (and 0.4.29-alpha builds) wrote model files
        // without a loader marker. They must be treated as not
        // downloaded so the next Download press forces a clean
        // re-fetch from upstream.
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()

        val modelDir = File(ctx.filesDir, "local_tts/models/${model.id}")
        modelDir.mkdirs()
        File(modelDir, model.onnxFileName).writeBytes(ByteArray(16) { 1 })
        File(modelDir, model.onnxJsonFileName).writeBytes(ByteArray(16) { 1 })
        File(modelDir, "tokens.txt").writeBytes(ByteArray(16) { 1 })
        // No .local-tts-loader-v4 file.

        assertFalse(downloader.isModelDownloaded(model))
    }

    @Test
    fun `isModelDownloaded returns true when both files are present and non-empty`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val model = VoiceModelRegistry.availableVoices.first()

        val modelDir = File(ctx.filesDir, "local_tts/models/${model.id}")
        modelDir.mkdirs()
        writeFakePiperModel(modelDir, model)
        // Current loader marker must also be present — older installs
        // that lack it are treated as not downloaded and re-fetched.
        File(modelDir, ".local-tts-loader-v4").writeText("loader-v4")

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
        writeFakePiperModel(firstDir, first)
        File(firstDir, ".local-tts-loader-v4").writeText("loader-v4")

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

    @Test
    fun `generateTokensFile produces one token per line in id order`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val tmpDir = tmp.newFolder("tokens_ok")
        val json = File(tmpDir, "model.onnx.json")
        // 5 tokens with non-sorted JSON key order
        json.writeText(
            """
            {
              "audio": {"sample_rate": 22050, "quality": "medium"},
              "phoneme_id_map": {
                "_": [0],
                "a": [2],
                " ": [1],
                "b": [3],
                "^": [4]
              }
            }
            """.trimIndent()
        )
        val out = File(tmpDir, "tokens.txt")
        val result = downloader.generateTokensFile(json, out)
        assertTrue("generateTokensFile failed: $result", result.isSuccess)
        val lines = out.readLines()
        // Sherpa-ONNX's Piper token parser expects each line to be
        // "<token> <id>" — except the space token, which is encoded
        // as a single numeric column with the id alone (the parser
        // treats a single-column numeric line as the space token).
        assertEquals(listOf("_ 0", "1", "a 2", "b 3", "^ 4"), lines)
    }

    @Test
    fun `generateTokensFile fails when phoneme_id_map is missing`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val tmpDir = tmp.newFolder("tokens_no_map")
        val json = File(tmpDir, "model.onnx.json")
        json.writeText("""{"audio": {"sample_rate": 22050}}""")
        val out = File(tmpDir, "tokens.txt")
        val result = downloader.generateTokensFile(json, out)
        assertTrue(result.isFailure)
        assertFalse(out.exists())
    }

    @Test
    fun `generateTokensFile fails when ids are not contiguous`() {
        val ctx = RuntimeEnvironment.getApplication()
        val downloader = OnnxModelDownloader(ctx)
        val tmpDir = tmp.newFolder("tokens_gap")
        val json = File(tmpDir, "model.onnx.json")
        // Gap at id=1
        json.writeText(
            """{"phoneme_id_map": {"_": [0], "a": [2]}}"""
        )
        val out = File(tmpDir, "tokens.txt")
        val result = downloader.generateTokensFile(json, out)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("expected gap message, got: $msg", msg.contains("gaps"))
    }

    /**
     * Write a fake Piper model directory that passes [OnnxModelDownloader.validateModelFiles]:
     * - the onnx stub is at least max(MIN_ONNX_BYTES=1MB, model.sizeBytes/5) (the
     *   current models in the registry are ~63MB, so the min is ~12.6MB). We pad
     *   to 16MB to cover the full set without per-model arithmetic in the test.
     * - the prefix is non-ASCII-printable so it cannot be a Git-LFS pointer or HTML
     * - the json file is a real Piper .onnx.json with a non-empty phoneme_id_map
     * - the tokens file is non-empty
     */
    private fun writeFakePiperModel(modelDir: File, model: VoiceModel) {
        val onnxBytes = ByteArray(16_000_000) { (it and 0x7F).toByte() }
        File(modelDir, model.onnxFileName).writeBytes(onnxBytes)
        File(modelDir, model.onnxJsonFileName).writeText(
            """
            {
              "audio": { "sample_rate": 22050, "quality": "medium" },
              "phoneme_id_map": {
                "_": [0],
                "^": [1],
                "a": [2],
                " ": [3],
                "b": [4]
              }
            }
            """.trimIndent()
        )
        File(modelDir, "tokens.txt").writeBytes(ByteArray(64) { 1 })
    }
}
