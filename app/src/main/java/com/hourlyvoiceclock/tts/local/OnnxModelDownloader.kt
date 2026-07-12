package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
import com.hourlyvoiceclock.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.buffer
import okio.source
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import kotlin.math.max

/**
 * Downloads Piper voice distributions for use with Sherpa-ONNX's VITS
 * offline TTS. A loadable distribution needs three things on disk:
 *
 *   1. `model.onnx`            — the ONNX model weights
 *   2. `model.onnx.json`       — audio config + phoneme id map
 *   3. `tokens.txt`            — one token per line, id-ordered, derived
 *                                from `model.onnx.json`
 *
 * The espeak-ng-data phoneme directory is bundled in APK assets and
 * loaded via [com.hourlyvoiceclock.tts.local.LocalTtsEngine] — it is
 * shared across all voices and not downloaded per-voice.
 *
 * The "already downloaded" check validates the local files enough to reject
 * partial downloads, HTML error pages, Git-LFS pointer files, and malformed
 * Piper JSON. It also requires the current loader marker so models touched by
 * older unsafe metadata patches are re-downloaded from clean upstream files.
 *
 * Errors are returned as a [Result] and re-thrown as a typed
 * [DownloadException] so the caller can render a human-readable error
 * in the UI without parsing log lines.
 */
open class OnnxModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelsDir: File
        get() = File(context.filesDir, "local_tts/models")

    suspend fun downloadModel(
        model: VoiceModel,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val modelDir = File(modelsDir, model.id)
        val onnxFile = File(modelDir, model.onnxFileName)
        val jsonFile = File(modelDir, model.onnxJsonFileName)
        val tokensFile = File(modelDir, "tokens.txt")
        val markerFile = File(modelDir, CURRENT_LOADER_MARKER)

        if (isModelDownloaded(model)) {
            Log.d(TAG, "Model ${model.id} already downloaded")
            return@withContext Result.success(onnxFile)
        }

        // Clean any partial, stale, or invalid state from a previous attempt.
        if (modelDir.exists()) {
            onnxFile.delete()
            jsonFile.delete()
            tokensFile.delete()
            markerFile.delete()
        } else {
            modelDir.mkdirs()
        }

        try {
            if (model.archiveDownloadUrl != null) {
                // Archive-backed voice (BibEBobberson-style .zip / .tar.gz /
                // .tgz). Stream the whole archive to a temp file, then
                // extract the first .onnx + matching .onnx.json into the
                // canonical filenames.
                val archiveResult = downloadArchiveAndExtract(
                    model = model,
                    modelDir = modelDir,
                    onnxFile = onnxFile,
                    jsonFile = jsonFile,
                    onProgress = onProgress
                )
                if (archiveResult.isFailure) {
                    return@withContext Result.failure(archiveResult.exceptionOrNull()!!)
                }
            } else {
                // Direct-pair voice (default path). Two HTTP GETs.
                // .onnx -> 0.0 .. 0.85
                val onnxResult = downloadOne(model.onnxDownloadUrl, onnxFile) { fraction ->
                    onProgress(fraction * 0.85f)
                }
                if (onnxResult.isFailure) {
                    onnxFile.delete()
                    return@withContext Result.failure(onnxResult.exceptionOrNull()!!)
                }

                // .onnx.json -> 0.85 .. 0.98
                val jsonResult = downloadOne(model.onnxJsonDownloadUrl, jsonFile) { fraction ->
                    onProgress(0.85f + fraction * 0.13f)
                }
                if (jsonResult.isFailure) {
                    onnxFile.delete()
                    jsonFile.delete()
                    return@withContext Result.failure(jsonResult.exceptionOrNull()!!)
                }
            }

            // Generate tokens.txt from the phoneme id map in the JSON.
            onProgress(0.98f)
            val tokensResult = generateTokensFile(jsonFile, tokensFile)
            if (tokensResult.isFailure) {
                onnxFile.delete()
                jsonFile.delete()
                tokensFile.delete()
                return@withContext Result.failure(tokensResult.exceptionOrNull()!!)
            }

            val validation = validateModelFiles(model, onnxFile, jsonFile, tokensFile)
            if (validation.isFailure) {
                onnxFile.delete()
                jsonFile.delete()
                tokensFile.delete()
                markerFile.delete()
                return@withContext Result.failure(validation.exceptionOrNull()!!)
            }

            markerFile.writeText(CURRENT_LOADER_MARKER)

            onProgress(1f)
            Log.d(
                TAG,
                "Downloaded model ${model.id} " +
                    "(onnx=${onnxFile.length()}, json=${jsonFile.length()}, " +
                    "tokens=${tokensFile.length()})"
            )
            Result.success(onnxFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model ${model.id}", e)
            onnxFile.delete()
            jsonFile.delete()
            tokensFile.delete()
            markerFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Build a Sherpa-ONNX-compatible `tokens.txt` from the
     * `phoneme_id_map` object in `MODEL.onnx.json`. One token per line,
     * ordered by integer token id (line N corresponds to id N). Returns
     * failure if the JSON is malformed or the map is missing.
     */
    internal fun generateTokensFile(
        jsonFile: File,
        tokensFile: File
    ): Result<Unit> {
        return try {
            val payload = jsonFile.source().buffer().use { src: BufferedSource ->
                src.readUtf8()
            }
            val root = JSONObject(payload)
            val map = root.optJSONObject("phoneme_id_map")
                ?: return phonemeMapFailure("phoneme_id_map missing in ${jsonFile.name}")

            // Build a list of (id, token) pairs and sort by id.
            val entries = mutableListOf<Pair<Int, String>>()
            val keys = map.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val idArray = map.optJSONArray(key)
                    ?: return phonemeMapFailure("phoneme_id_map entry '$key' is not an array")
                // Piper may map a single key to multiple ids (variants).
                // The C++ side reads the first id only, so we use the
                // first element for the canonical token id.
                val id = idArray.optInt(0, -1)
                if (id < 0) {
                    return phonemeMapFailure("phoneme_id_map entry '$key' has no integer id")
                }
                entries.add(id to key)
            }

            val byId = entries.sortedBy { it.first }
            val maxId = entries.maxOfOrNull { it.first }
            if (maxId == null) {
                return phonemeMapFailure("phoneme_id_map is empty in ${jsonFile.name}")
            }
            if (maxId < 0) {
                return phonemeMapFailure("phoneme_id_map has invalid ids in ${jsonFile.name}")
            }
            // Verify no gaps: every id from 0..maxId must appear.
            val seen = BooleanArray(maxId + 1)
            entries.forEach { (id, _) -> seen[id] = true }
            val missing = (0..maxId).filter { !seen[it] }
            if (missing.isNotEmpty()) {
                return phonemeMapFailure(
                    "phoneme_id_map has gaps (missing ids: ${missing.take(8)}" +
                        (if (missing.size > 8) "..." else "") +
                        "); cannot build a contiguous tokens.txt"
                )
            }

            tokensFile.bufferedWriter(Charsets.UTF_8).use { out ->
                byId.forEach { (id, token) ->
                    if (token == " ") {
                        // Sherpa's Piper token parser treats a single-column
                        // numeric line as the space token with that id.
                        out.write(id.toString())
                    } else {
                        out.write(token)
                        out.write(' '.code)
                        out.write(id.toString())
                    }
                    out.newLine()
                }
            }
            Result.success(Unit)
        } catch (e: IOException) {
            tokensFile.delete()
            Result.failure(
                DownloadException(
                    context.getString(R.string.download_error_tokens_io, e.message ?: ""),
                    e
                )
            )
        } catch (e: Exception) {
            tokensFile.delete()
            Result.failure(
                DownloadException(
                    context.getString(
                        R.string.download_error_tokens_failed,
                        e.message ?: e.javaClass.simpleName
                    ),
                    e
                )
            )
        }
    }

    /** Phoneme-map details stay in logs; UI gets a generic corrupt-model string. */
    private fun phonemeMapFailure(detail: String): Result<Unit> {
        Log.e(TAG, detail)
        return Result.failure(
            DownloadException(context.getString(R.string.download_error_model_corrupt))
        )
    }

    internal fun validateModelFiles(
        model: VoiceModel,
        onnxFile: File,
        jsonFile: File,
        tokensFile: File
    ): Result<Unit> {
        val name = model.displayName(context)
        if (!onnxFile.exists() || onnxFile.length() <= 0L) {
            return Result.failure(
                DownloadException(context.getString(R.string.download_error_onnx_missing, name))
            )
        }
        if (!jsonFile.exists() || jsonFile.length() <= 0L) {
            return Result.failure(
                DownloadException(context.getString(R.string.download_error_json_missing, name))
            )
        }
        if (!tokensFile.exists() || tokensFile.length() <= 0L) {
            return Result.failure(
                DownloadException(context.getString(R.string.download_error_tokens_missing, name))
            )
        }

        val minExpectedBytes = max(MIN_ONNX_BYTES, model.sizeBytes / 5L)
        if (onnxFile.length() < minExpectedBytes) {
            return Result.failure(
                DownloadException(
                    context.getString(
                        R.string.download_error_onnx_too_small,
                        name,
                        onnxFile.length(),
                        minExpectedBytes
                    )
                )
            )
        }

        val prefix = readPrefix(onnxFile).trimStart()
        if (prefix.startsWith("version https://git-lfs.github.com/spec/v1")) {
            return Result.failure(
                DownloadException(context.getString(R.string.download_error_lfs_pointer, name))
            )
        }
        if (prefix.startsWith("<") || prefix.startsWith("{") || prefix.startsWith("<!DOCTYPE", ignoreCase = true)) {
            return Result.failure(
                DownloadException(context.getString(R.string.download_error_not_onnx, name))
            )
        }

        return try {
            val payload = jsonFile.source().buffer().use { it.readUtf8() }
            val root = JSONObject(payload)
            val map = root.optJSONObject("phoneme_id_map")
            if (map == null) {
                Log.e(TAG, "phoneme_id_map missing in ${jsonFile.name}")
                return Result.failure(
                    DownloadException(context.getString(R.string.download_error_model_corrupt))
                )
            }
            if (map.length() == 0) {
                Log.e(TAG, "phoneme_id_map is empty in ${jsonFile.name}")
                return Result.failure(
                    DownloadException(context.getString(R.string.download_error_model_corrupt))
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                DownloadException(
                    context.getString(
                        R.string.download_error_invalid_json,
                        name,
                        e.message ?: e.javaClass.simpleName
                    ),
                    e
                )
            )
        }
    }

    suspend fun isModelDownloadedAsync(model: VoiceModel): Boolean = withContext(Dispatchers.IO) {
        isModelDownloaded(model)
    }

    open fun isModelDownloaded(model: VoiceModel): Boolean {
        val modelDir = File(modelsDir, model.id)
        val onnx = File(modelDir, model.onnxFileName)
        val json = File(modelDir, model.onnxJsonFileName)
        val tokens = File(modelDir, "tokens.txt")
        val marker = File(modelDir, CURRENT_LOADER_MARKER)

        if (!marker.exists()) {
            if (onnx.exists() || json.exists() || tokens.exists()) {
                Log.w(TAG, "Ignoring ${model.id}: missing current loader marker; re-download required")
            }
            return false
        }

        val valid = validateModelFiles(model, onnx, json, tokens).isSuccess
        if (!valid && (onnx.exists() || json.exists() || tokens.exists())) {
            Log.w(TAG, "Ignoring invalid local model files for ${model.id}")
        }
        return valid
    }

    fun deleteModel(model: VoiceModel): Boolean {
        val modelDir = File(modelsDir, model.id)
        return modelDir.deleteRecursively()
    }

    open fun getDownloadedModels(): List<VoiceModel> {
        return VoiceModelRegistry.availableVoices.filter { isModelDownloaded(it) }
    }

    fun getTotalDownloadedSize(): Long {
        return modelsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private suspend fun downloadOne(
        url: String,
        target: File,
        onFraction: (Float) -> Unit
    ): Result<Unit> {
        val request = Request.Builder().url(url).build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            return Result.failure(
                DownloadException(
                    context.getString(
                        R.string.download_error_network,
                        e.message ?: e.javaClass.simpleName
                    ),
                    e
                )
            )
        }

        response.use { r ->
            if (!r.isSuccessful) {
                Log.e(TAG, "HTTP ${r.code} for $url")
                return Result.failure(
                    DownloadException(context.getString(R.string.download_error_http, r.code))
                )
            }
            val body = r.body
                ?: return Result.failure(
                    DownloadException(context.getString(R.string.download_error_empty_body))
                )

            val totalBytes = body.contentLength()
            var downloaded = 0L
            try {
                body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            currentCoroutineContext().ensureActive()
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                onFraction(downloaded.toFloat() / totalBytes)
                            }
                        }
                        output.flush()
                    }
                }
            } catch (e: IOException) {
                target.delete()
                return Result.failure(
                    DownloadException(
                        context.getString(
                            R.string.download_error_io,
                            e.message ?: e.javaClass.simpleName
                        ),
                        e
                    )
                )
            }
        }
        return Result.success(Unit)
    }

    private fun readPrefix(file: File): String {
        val buffer = ByteArray(PREFIX_BYTES)
        val read = file.inputStream().use { it.read(buffer) }
        if (read <= 0) return ""
        return buffer.copyOf(read).toString(Charsets.UTF_8)
    }

    /**
     * Download a voice archive (.zip, .tar.gz, or .tgz) and extract the
     * first `.onnx` model plus its matching `.onnx.json` (or fallback
     * `config.json`) into [onnxFile] / [jsonFile]. The rest of the
     * pipeline (`tokens.txt`, `validateModelFiles`, marker write)
     * continues unchanged because the canonical filenames are now
     * populated.
     *
     * Archive layout expectations:
     *  - one file whose path ends in `.onnx`
     *  - one file whose path ends in `.onnx.json`, or a sibling
     *    `config.json` (Coqui/Piper convention used by some authors)
     *
     * On any failure the partially-written [onnxFile] / [jsonFile] are
     * deleted so the next attempt starts from a clean model directory.
     */
    private suspend fun downloadArchiveAndExtract(
        model: VoiceModel,
        modelDir: File,
        onnxFile: File,
        jsonFile: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        val archiveUrl = model.archiveDownloadUrl ?: return Result.failure(
            DownloadException(context.getString(R.string.download_error_archive_type))
        )

        val ext = archiveUrl.substringAfterLast('?', "")
            .substringAfterLast('/').lowercase()
            .let { filename ->
                when {
                    filename.endsWith(".tar.gz") -> "tar.gz"
                    filename.endsWith(".tgz") -> "tgz"
                    filename.endsWith(".zip") -> "zip"
                    else -> "unknown"
                }
            }
        if (ext == "unknown") {
            Log.e(TAG, "Archive type not recognised for ${model.id}: $archiveUrl")
            return Result.failure(
                DownloadException(context.getString(R.string.download_error_archive_type))
            )
        }

        // Stream the whole archive to a temp file. We can't pipe straight
        // into ZipInputStream / GZIPInputStream because we need to know the
        // archive's total length to drive the progress callback, and we
        // need seekable random access to skip tar headers efficiently.
        val tempArchive = File.createTempFile("voice-${model.id}-", ".$ext", modelDir)
        val outcome: Result<Unit> = try {
            // 0.0 .. 0.85 for the raw download, 0.85 .. 0.98 for the extract.
            val downloadResult = downloadOne(archiveUrl, tempArchive) { fraction ->
                onProgress(fraction * 0.85f)
            }
            if (downloadResult.isFailure) {
                downloadResult
            } else {
                onProgress(0.86f)
                val extractResult = extractArchive(tempArchive, onnxFile, jsonFile)
                if (extractResult.isFailure) {
                    onnxFile.delete()
                    jsonFile.delete()
                } else {
                    Log.d(
                        TAG,
                        "Extracted ${model.id} from $ext archive " +
                            "(onnx=${onnxFile.length()}, json=${jsonFile.length()})"
                    )
                }
                extractResult
            }
        } finally {
            tempArchive.delete()
        }
        return outcome
    }

    /**
     * Extract a voice archive (.zip, .tar.gz, or .tgz) on disk to
     * [onnxOut] / [jsonOut]. Public seam used by the [downloadArchiveAndExtract]
     * pipeline and by tests that build synthetic archives.
     *
     * Archive layout expectations:
     *  - one file whose path ends in `.onnx`
     *  - one file whose path ends in `.onnx.json`, or a sibling
     *    `config.json` (Coqui/Piper convention used by some authors)
     *
     * On failure the partially-written [onnxOut] / [jsonOut] are
     * deleted so the caller can start from a clean directory.
     */
    internal fun extractArchive(
        archive: File,
        onnxOut: File,
        jsonOut: File
    ): Result<Unit> {
        val name = archive.name.lowercase()
        return try {
            when {
                name.endsWith(".zip") -> extractFromZip(archive, onnxOut, jsonOut)
                name.endsWith(".tar.gz") || name.endsWith(".tgz") ->
                    extractFromTarGz(archive, onnxOut, jsonOut)
                else -> Result.failure(
                    DownloadException(context.getString(R.string.download_error_archive_type))
                )
            }
        } catch (e: Exception) {
            Result.failure(
                DownloadException(
                    context.getString(
                        R.string.download_error_archive_extract,
                        e.message ?: e.javaClass.simpleName
                    ),
                    e
                )
            )
        }
    }

    /**
     * Walk a .zip archive sequentially, writing the first `.onnx` entry
     * to [onnxOut] and the first `.onnx.json` (or `config.json`) entry
     * to [jsonOut]. Fails if either is missing; partial outputs are
     * cleaned up so the caller can start from a clean directory.
     */
    private fun extractFromZip(
        archive: File,
        onnxOut: File,
        jsonOut: File
    ): Result<Unit> {
        var foundOnnx = false
        var foundJson = false
        try {
            archive.inputStream().use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.substringAfterLast('/')
                        when {
                            !foundOnnx && name.endsWith(".onnx") && !entry.isDirectory -> {
                                onnxOut.outputStream().use { out -> zis.copyTo(out) }
                                foundOnnx = true
                            }
                            !foundJson && !entry.isDirectory && (
                                name.endsWith(".onnx.json") || name == "config.json"
                            ) -> {
                                jsonOut.outputStream().use { out -> zis.copyTo(out) }
                                foundJson = true
                            }
                        }
                        if (foundOnnx && foundJson) break
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            onnxOut.delete()
            jsonOut.delete()
            throw e
        }
        return when {
            !foundOnnx -> {
                onnxOut.delete()
                jsonOut.delete()
                Result.failure(
                    DownloadException(context.getString(R.string.download_error_archive_no_onnx))
                )
            }
            !foundJson -> {
                onnxOut.delete()
                jsonOut.delete()
                Result.failure(
                    DownloadException(context.getString(R.string.download_error_archive_no_json))
                )
            }
            else -> Result.success(Unit)
        }
    }

    /**
     * Walk a .tar (or .tar.gz after GZIP decompression) archive, writing
     * the first `.onnx` entry to [onnxOut] and the first `.onnx.json`
     * (or `config.json`) entry to [jsonOut]. Fails if either is missing
     * or the tar header is malformed.
     *
     * Tar entries use 512-byte headers with named fields at fixed
     * offsets (POSIX.1-1988 / `ustar`). This implementation only reads
     * the subset we need: name, size, and end-of-archive (two
     * consecutive 512-byte zero blocks).
     */
    private fun extractFromTarGz(
        archive: File,
        onnxOut: File,
        jsonOut: File
    ): Result<Unit> {
        var foundOnnx = false
        var foundJson = false
        try {
            archive.inputStream().use { fis ->
                GZIPInputStream(fis).use { gz ->
                    val tar = BufferedInputStream(gz)
                    val header = ByteArray(512)
                    while (true) {
                        val read = readFully(tar, header, 0, 512)
                        if (read == 0) break
                        if (read < 512) {
                            Log.e(TAG, "Truncated tar header (read $read bytes)")
                            return Result.failure(
                                DownloadException(
                                    context.getString(R.string.download_error_archive_extract, "truncated header")
                                )
                            )
                        }
                        // End-of-archive: two consecutive zero blocks. We only
                        // need to check the first.
                        if (header.isAllZeros()) break

                        val name = readTarString(header, 0, 100)
                        val sizeStr = readTarString(header, 124, 12).trim()
                        // Tar sizes are octal (POSIX ustar). Parse base 8.
                        val size = sizeStr.toLongOrNull(8) ?: run {
                            Log.e(TAG, "Bad tar size '$sizeStr' for entry '$name'")
                            return Result.failure(
                                DownloadException(
                                    context.getString(R.string.download_error_archive_extract, "bad tar size")
                                )
                            )
                        }
                        // Round up to 512-byte boundary.
                        val blocks = ((size + 511) / 512).toInt()
                        val dataBytes = blocks * 512

                        val baseName = name.substringAfterLast('/')
                        val isOnnx = !foundOnnx && baseName.endsWith(".onnx")
                        val isJson = !foundJson && (
                            baseName.endsWith(".onnx.json") || baseName == "config.json"
                        )

                        if (isOnnx || isJson) {
                            val out = if (isOnnx) onnxOut else jsonOut
                            if (isOnnx) foundOnnx = true else foundJson = true
                            out.outputStream().use { sink ->
                                tar.copyToLimited(sink, size)
                            }
                            // Skip any padding that rounded the entry
                            // up to a 512-byte boundary. Without this
                            // the next header read lands inside the
                            // padding zeros.
                            val pad = dataBytes - size
                            if (pad > 0) tar.skipNBytesCompat(pad)
                        } else {
                            tar.skipNBytesCompat(dataBytes.toLong())
                        }

                        if (foundOnnx && foundJson) break
                    }
                }
            }
        } catch (e: Exception) {
            onnxOut.delete()
            jsonOut.delete()
            throw e
        }
        return when {
            !foundOnnx -> {
                onnxOut.delete()
                jsonOut.delete()
                Result.failure(
                    DownloadException(context.getString(R.string.download_error_archive_no_onnx))
                )
            }
            !foundJson -> {
                onnxOut.delete()
                jsonOut.delete()
                Result.failure(
                    DownloadException(context.getString(R.string.download_error_archive_no_json))
                )
            }
            else -> Result.success(Unit)
        }
    }

    /** Read exactly [len] bytes from [src] into [buf] starting at [off]. */
    private fun readFully(src: InputStream, buf: ByteArray, off: Int, len: Int): Int {
        var total = 0
        while (total < len) {
            val n = src.read(buf, off + total, len - total)
            if (n < 0) return if (total == 0) 0 else total
            total += n
        }
        return total
    }

    /** Copy exactly [count] bytes from this stream to [out]. */
    private fun InputStream.copyToLimited(out: java.io.OutputStream, count: Long) {
        val buf = ByteArray(8192)
        var remaining = count
        while (remaining > 0) {
            val toRead = if (remaining < buf.size) remaining.toInt() else buf.size
            val read = this.read(buf, 0, toRead)
            if (read < 0) throw IOException("Unexpected EOF in tar entry (wanted $count bytes)")
            out.write(buf, 0, read)
            remaining -= read
        }
    }

    /** Skip [n] bytes. Works on InputStream (skipNBytes is JDK 12+). */
    private fun InputStream.skipNBytesCompat(n: Long) {
        var remaining = n
        val buf = ByteArray(8192)
        while (remaining > 0) {
            val toRead = if (remaining < buf.size) remaining.toInt() else buf.size
            val read = this.read(buf, 0, toRead)
            if (read < 0) throw IOException("Unexpected EOF in tar skip (wanted $n bytes)")
            remaining -= read
        }
    }

    /**
     * Decode a NUL-terminated ASCII tar string field. Tar string fields
     * are right-padded with NULs and never contain UTF-8.
     */
    private fun readTarString(buf: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val limit = offset + length
        while (end < limit && buf[end] != 0.toByte()) end++
        return String(buf, offset, end - offset, Charsets.US_ASCII)
    }

    private fun ByteArray.isAllZeros(): Boolean {
        for (b in this) if (b != 0.toByte()) return false
        return true
    }

    companion object {
        private const val TAG = "OnnxModelDownloader"
        private const val MIN_ONNX_BYTES = 1_000_000L
        private const val PREFIX_BYTES = 512
        private const val CURRENT_LOADER_MARKER = ".local-tts-loader-v4"
    }
}

class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
