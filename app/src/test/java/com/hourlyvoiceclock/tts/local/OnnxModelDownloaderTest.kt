package com.hourlyvoiceclock.tts.local

import org.junit.Assert.assertArrayEquals
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
            // Every model must have either:
            //  - a direct .onnx + .onnx.json pair (HTTPS, names match), or
            //  - an archive (.zip / .tar.gz / .tgz) URL that the
            //    downloader extracts the pair from.
            // The two are mutually exclusive; archive-backed voices
            // legitimately have an empty `onnxJsonDownloadUrl`.
            assertTrue(
                "Model ${model.id} onnxDownloadUrl must be https: ${model.onnxDownloadUrl}",
                model.onnxDownloadUrl.startsWith("https://")
            )
            assertTrue(
                "Model ${model.id} onnxFileName should end with .onnx: ${model.onnxFileName}",
                model.onnxFileName.endsWith(".onnx")
            )
            assertTrue(
                "Model ${model.id} onnxJsonFileName should end with .onnx.json: ${model.onnxJsonFileName}",
                model.onnxJsonFileName.endsWith(".onnx.json")
            )
            if (model.archiveDownloadUrl == null) {
                // Direct-pair model: both URLs must be present and
                // independently HTTPS.
                assertTrue(
                    "Model ${model.id} onnxJsonDownloadUrl must be https: ${model.onnxJsonDownloadUrl}",
                    model.onnxJsonDownloadUrl.startsWith("https://")
                )
            } else {
                // Archive model: archive URL must also be HTTPS and end
                // with a supported archive extension.
                assertTrue(
                    "Model ${model.id} archiveDownloadUrl must be https: ${model.archiveDownloadUrl}",
                    model.archiveDownloadUrl!!.startsWith("https://")
                )
                val archiveName = model.archiveDownloadUrl.substringAfterLast('/')
                assertTrue(
                    "Model ${model.id} archive must be .zip / .tar.gz / .tgz: $archiveName",
                    archiveName.endsWith(".zip") ||
                        archiveName.endsWith(".tar.gz") ||
                        archiveName.endsWith(".tgz")
                )
            }
        }
    }

    @Test
    fun `model has matching onnx and json file names`() {
        VoiceModelRegistry.availableVoices.forEach { model ->
            // For direct-pair models the URL tail must match the
            // canonical filename. For archive-backed models the URL is
            // an archive so we skip this consistency check.
            if (model.archiveDownloadUrl != null) return@forEach
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

    // ================================================================
    // Archive-backed voice tests (.tar.gz, .zip)
    // ================================================================
    //
    // These tests don't hit the network — they build a small archive
    // on disk that contains a fake .onnx + .onnx.json, then exercise
    // the extract helper directly. The extraction code uses
    // java.util.zip.GZIPInputStream and ZipInputStream, so the
    // synthetic archives go through the exact same path real voices
    // would.

    private fun buildFakeTarGz(
        archive: File,
        entries: List<Pair<String, ByteArray>>
    ) {
        // Build a raw tar stream with 512-byte headers + 512-byte-aligned
        // data, then gzip it. We avoid a third-party tar library by
        // hand-rolling the header: the format is fixed-offset and only
        // the fields we use matter.
        java.io.ByteArrayOutputStream().use { tarBytes ->
            for ((name, data) in entries) {
                val nameBytes = name.toByteArray(Charsets.US_ASCII)
                require(nameBytes.size < 100) { "test entry name too long: $name" }
                val header = ByteArray(512)
                System.arraycopy(nameBytes, 0, header, 0, nameBytes.size)
                val sizeOctal = java.lang.Long.toOctalString(data.size.toLong()).toByteArray(Charsets.US_ASCII)
                System.arraycopy(sizeOctal, 0, header, 124, sizeOctal.size)
                header[148] = '0'.code.toByte() // mode
                // Magic + version for ustar (the loader doesn't strictly
                // require it but real tar archives include it).
                val magic = "ustar\u0000".toByteArray(Charsets.US_ASCII)
                System.arraycopy(magic, 0, header, 257, magic.size)
                val version = "00".toByteArray(Charsets.US_ASCII)
                System.arraycopy(version, 0, header, 263, version.size)
                // Header checksum: sum of all bytes treating the
                // chksum field (148..155) as spaces (matches GNU tar's
                // convention). Save/restore the size field because the
                // extractor reads it back from the same buffer.
                for (i in 148..155) header[i] = ' '.code.toByte()
                var sum = 0
                for (b in header) sum += b.toInt() and 0xff
                val chkOctal = String.format("%06o\u0000 ", sum).toByteArray(Charsets.US_ASCII)
                System.arraycopy(chkOctal, 0, header, 148, chkOctal.size)
                tarBytes.write(header)
                tarBytes.write(data)
                val padLen = ((512 - (data.size % 512)) % 512)
                if (padLen > 0) tarBytes.write(ByteArray(padLen))
            }
            // Two 512-byte zero blocks to terminate the archive.
            tarBytes.write(ByteArray(1024))
            java.io.FileOutputStream(archive).use { fos ->
                java.util.zip.GZIPOutputStream(fos).use { gz ->
                    gz.write(tarBytes.toByteArray())
                }
            }
        }
    }

    private fun buildFakeZip(
        archive: File,
        entries: List<Pair<String, ByteArray>>
    ) {
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(archive)).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
    }

    @Test
    fun `extractArchive pulls the onnx and onnx-json out of a tar-gz`() {
        val modelDir = tmp.newFolder("model-tgz")
        val onnxOut = File(modelDir, "en_US-test-medium.onnx")
        val jsonOut = File(modelDir, "en_US-test-medium.onnx.json")
        val archive = File(modelDir, "test.tar.gz")

        val fakeOnnx = ByteArray(2_000_000) { (it and 0xff).toByte() }
        val fakeJson = """{"phoneme_id_map":{"a":[1]}}""".toByteArray()

        buildFakeTarGz(
            archive,
            listOf(
                "en_US-test-medium.onnx" to fakeOnnx,
                "en_US-test-medium.onnx.json" to fakeJson
            )
        )

        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val result = downloader.extractArchive(archive, onnxOut, jsonOut)
        assertTrue("extract failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals(fakeOnnx.size.toLong(), onnxOut.length())
        assertEquals(fakeJson.size.toLong(), jsonOut.length())
        // The bytes must round-trip exactly; in particular the .onnx
        // content must not have been replaced by the .onnx.json.
        assertArrayEquals(fakeOnnx, onnxOut.readBytes())
        assertArrayEquals(fakeJson, jsonOut.readBytes())
    }

    @Test
    fun `extractArchive pulls the onnx and onnx-json out of a zip`() {
        val modelDir = tmp.newFolder("model-zip")
        val onnxOut = File(modelDir, "en_US-test-medium.onnx")
        val jsonOut = File(modelDir, "en_US-test-medium.onnx.json")
        val archive = File(modelDir, "test.zip")

        val fakeOnnx = ByteArray(2_000_000) { (it and 0xff).toByte() }
        val fakeJson = """{"phoneme_id_map":{"a":[1]}}""".toByteArray()

        buildFakeZip(
            archive,
            listOf(
                "en_US-test-medium.onnx" to fakeOnnx,
                "en_US-test-medium.onnx.json" to fakeJson
            )
        )

        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val result = downloader.extractArchive(archive, onnxOut, jsonOut)
        assertTrue("extract failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals(fakeOnnx.size.toLong(), onnxOut.length())
        assertEquals(fakeJson.size.toLong(), jsonOut.length())
        assertArrayEquals(fakeOnnx, onnxOut.readBytes())
        assertArrayEquals(fakeJson, jsonOut.readBytes())
    }

    @Test
    fun `extractArchive fails when the archive has no onnx file`() {
        val modelDir = tmp.newFolder("model-no-onnx")
        val onnxOut = File(modelDir, "en_US-test-medium.onnx")
        val jsonOut = File(modelDir, "en_US-test-medium.onnx.json")
        val archive = File(modelDir, "test.zip")

        buildFakeZip(
            archive,
            listOf("en_US-test-medium.onnx.json" to "{\"phoneme_id_map\":{}}".toByteArray())
        )

        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val result = downloader.extractArchive(archive, onnxOut, jsonOut)
        assertTrue(result.isFailure)
        assertFalse(onnxOut.exists())
        assertFalse(jsonOut.exists())
    }

    @Test
    fun `extractArchive fails when the archive has no onnx-json file`() {
        val modelDir = tmp.newFolder("model-no-json")
        val onnxOut = File(modelDir, "en_US-test-medium.onnx")
        val jsonOut = File(modelDir, "en_US-test-medium.onnx.json")
        val archive = File(modelDir, "test.zip")

        val fakeOnnx = ByteArray(2_000_000) { 0 }
        buildFakeZip(
            archive,
            listOf("en_US-test-medium.onnx" to fakeOnnx)
        )

        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val result = downloader.extractArchive(archive, onnxOut, jsonOut)
        assertTrue(result.isFailure)
        assertFalse(onnxOut.exists())
        assertFalse(jsonOut.exists())
    }

    @Test
    fun `extractArchive accepts a config-json sibling in place of onnx-json`() {
        // Some authors (Coqui/Piper convention) ship a `config.json`
        // rather than an `en_US-<name>.onnx.json`. The extractor
        // should treat either as the JSON side of the pair.
        val modelDir = tmp.newFolder("model-config-json")
        val onnxOut = File(modelDir, "en_US-test-medium.onnx")
        val jsonOut = File(modelDir, "en_US-test-medium.onnx.json")
        val archive = File(modelDir, "test.zip")

        val fakeOnnx = ByteArray(2_000_000) { 0 }
        val fakeConfig = """{"phoneme_id_map":{"a":[1]}}""".toByteArray()
        buildFakeZip(
            archive,
            listOf(
                "en_US-test-medium.onnx" to fakeOnnx,
                "config.json" to fakeConfig
            )
        )

        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val result = downloader.extractArchive(archive, onnxOut, jsonOut)
        assertTrue("extract failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals(fakeOnnx.size.toLong(), onnxOut.length())
        assertEquals(fakeConfig.size.toLong(), jsonOut.length())
        assertArrayEquals(fakeConfig, jsonOut.readBytes())
    }

    @Test
    fun `extractArchive handles a directory-prefixed tar entry`() {
        // Real-world tar archives often contain a leading directory
        // entry like `./` followed by the actual files. The directory
        // entry has typeflag='5', size=0, and must be skipped without
        // confusing the extractor.
        val modelDir = tmp.newFolder("model-dir-prefix")
        val onnxOut = File(modelDir, "en_US-test-medium.onnx")
        val jsonOut = File(modelDir, "en_US-test-medium.onnx.json")
        val archive = File(modelDir, "test.tar.gz")

        val fakeOnnx = ByteArray(2_000_000) { (it and 0xff).toByte() }
        val fakeJson = """{"phoneme_id_map":{"a":[1]}}""".toByteArray()

        // Build a tar that interleaves a directory entry between the
        // onnx and json files. The extractor must skip the directory
        // entry and keep matching the right filename suffixes.
        java.io.ByteArrayOutputStream().use { tarBytes ->
            fun writeEntry(name: String, data: ByteArray, typeFlag: Char = '0') {
                val nameBytes = name.toByteArray(Charsets.US_ASCII)
                require(nameBytes.size < 100) { "test entry name too long: $name" }
                val header = ByteArray(512)
                System.arraycopy(nameBytes, 0, header, 0, nameBytes.size)
                val sizeOct = java.lang.Long.toOctalString(data.size.toLong()).toByteArray(Charsets.US_ASCII)
                System.arraycopy(sizeOct, 0, header, 124, sizeOct.size)
                header[148] = '0'.code.toByte() // mode
                header[156] = typeFlag.code.toByte() // typeflag
                val magic = "ustar\u0000".toByteArray(Charsets.US_ASCII)
                System.arraycopy(magic, 0, header, 257, magic.size)
                val version = "00".toByteArray(Charsets.US_ASCII)
                System.arraycopy(version, 0, header, 263, version.size)
                for (i in 148..155) header[i] = ' '.code.toByte()
                var sum = 0
                for (b in header) sum += b.toInt() and 0xff
                val chkOct = String.format("%06o\u0000 ", sum).toByteArray(Charsets.US_ASCII)
                System.arraycopy(chkOct, 0, header, 148, chkOct.size)
                tarBytes.write(header)
                if (data.isNotEmpty()) tarBytes.write(data)
                val padLen = ((512 - (data.size % 512)) % 512)
                if (padLen > 0) tarBytes.write(ByteArray(padLen))
            }
            writeEntry("./", ByteArray(0), '5') // directory entry
            writeEntry("./en_US-test-medium.onnx", fakeOnnx)
            writeEntry("./en_US-test-medium.onnx.json", fakeJson)
            tarBytes.write(ByteArray(1024)) // end-of-archive

            java.io.FileOutputStream(archive).use { fos ->
                java.util.zip.GZIPOutputStream(fos).use { gz ->
                    gz.write(tarBytes.toByteArray())
                }
            }
        }

        val downloader = OnnxModelDownloader(RuntimeEnvironment.getApplication())
        val result = downloader.extractArchive(archive, onnxOut, jsonOut)
        assertTrue("extract failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals(fakeOnnx.size.toLong(), onnxOut.length())
        assertEquals(fakeJson.size.toLong(), jsonOut.length())
        assertArrayEquals(fakeOnnx, onnxOut.readBytes())
        assertArrayEquals(fakeJson, jsonOut.readBytes())
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
