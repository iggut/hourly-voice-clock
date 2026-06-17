package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
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
 * Piper JSON. Invalid existing files are treated as not installed so the next
 * Download press starts from a clean model directory.
 *
 * Errors are returned as a [Result] and re-thrown as a typed
 * [DownloadException] so the caller can render a human-readable error
 * in the UI without parsing log lines.
 */
class OnnxModelDownloader(private val context: Context) {

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

        if (isModelDownloaded(model)) {
            Log.d(TAG, "Model ${model.id} already downloaded")
            return@withContext Result.success(onnxFile)
        }

        // Clean any partial, stale, or invalid state from a previous attempt.
        if (modelDir.exists()) {
            onnxFile.delete()
            jsonFile.delete()
            tokensFile.delete()
        } else {
            modelDir.mkdirs()
        }

        try {
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
                return@withContext Result.failure(validation.exceptionOrNull()!!)
            }

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
                ?: return Result.failure(
                    DownloadException("phoneme_id_map missing in ${jsonFile.name}")
                )

            // Build a list of (id, token) pairs and sort by id.
            val entries = mutableListOf<Pair<Int, String>>()
            val keys = map.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val idArray = map.optJSONArray(key)
                    ?: return Result.failure(
                        DownloadException("phoneme_id_map entry '$key' is not an array")
                    )
                // Piper may map a single key to multiple ids (variants).
                // The C++ side reads the first id only, so we use the
                // first element for the canonical token id.
                val id = idArray.optInt(0, -1)
                if (id < 0) {
                    return Result.failure(
                        DownloadException("phoneme_id_map entry '$key' has no integer id")
                    )
                }
                entries.add(id to key)
            }

            val byId = entries.sortedBy { it.first }
            val maxId = entries.maxOfOrNull { it.first }
            if (maxId == null) {
                return Result.failure(
                    DownloadException("phoneme_id_map is empty in ${jsonFile.name}")
                )
            }
            if (maxId < 0) {
                return Result.failure(
                    DownloadException("phoneme_id_map has invalid ids in ${jsonFile.name}")
                )
            }
            // Verify no gaps: every id from 0..maxId must appear.
            val seen = BooleanArray(maxId + 1)
            entries.forEach { (id, _) -> seen[id] = true }
            val missing = (0..maxId).filter { !seen[it] }
            if (missing.isNotEmpty()) {
                return Result.failure(
                    DownloadException(
                        "phoneme_id_map has gaps (missing ids: ${missing.take(8)}" +
                            (if (missing.size > 8) "..." else "") +
                            "); cannot build a contiguous tokens.txt"
                    )
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
            Result.failure(DownloadException("I/O error generating tokens.txt: ${e.message}", e))
        } catch (e: Exception) {
            tokensFile.delete()
            Result.failure(DownloadException("Failed to generate tokens.txt: ${e.message ?: e.javaClass.simpleName}", e))
        }
    }

    internal fun validateModelFiles(
        model: VoiceModel,
        onnxFile: File,
        jsonFile: File,
        tokensFile: File
    ): Result<Unit> {
        if (!onnxFile.exists() || onnxFile.length() <= 0L) {
            return Result.failure(DownloadException("ONNX model missing for ${model.displayName}"))
        }
        if (!jsonFile.exists() || jsonFile.length() <= 0L) {
            return Result.failure(DownloadException("ONNX JSON missing for ${model.displayName}"))
        }
        if (!tokensFile.exists() || tokensFile.length() <= 0L) {
            return Result.failure(DownloadException("tokens.txt missing for ${model.displayName}"))
        }

        val minExpectedBytes = max(MIN_ONNX_BYTES, model.sizeBytes / 5L)
        if (onnxFile.length() < minExpectedBytes) {
            return Result.failure(
                DownloadException(
                    "Downloaded ONNX is too small for ${model.displayName}: " +
                        "${onnxFile.length()} bytes; expected at least $minExpectedBytes"
                )
            )
        }

        val prefix = readPrefix(onnxFile).trimStart()
        if (prefix.startsWith("version https://git-lfs.github.com/spec/v1")) {
            return Result.failure(DownloadException("Downloaded ${model.displayName} is a Git-LFS pointer, not ONNX weights"))
        }
        if (prefix.startsWith("<") || prefix.startsWith("{") || prefix.startsWith("<!DOCTYPE", ignoreCase = true)) {
            return Result.failure(DownloadException("Downloaded ${model.displayName} is not an ONNX binary"))
        }

        return try {
            val payload = jsonFile.source().buffer().use { it.readUtf8() }
            val root = JSONObject(payload)
            val map = root.optJSONObject("phoneme_id_map")
                ?: return Result.failure(DownloadException("phoneme_id_map missing in ${jsonFile.name}"))
            if (map.length() == 0) {
                return Result.failure(DownloadException("phoneme_id_map is empty in ${jsonFile.name}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DownloadException("Invalid ONNX JSON for ${model.displayName}: ${e.message ?: e.javaClass.simpleName}", e))
        }
    }

    suspend fun isModelDownloadedAsync(model: VoiceModel): Boolean = withContext(Dispatchers.IO) {
        isModelDownloaded(model)
    }

    fun isModelDownloaded(model: VoiceModel): Boolean {
        val onnx = File(modelsDir, "${model.id}/${model.onnxFileName}")
        val json = File(modelsDir, "${model.id}/${model.onnxJsonFileName}")
        val tokens = File(modelsDir, "${model.id}/tokens.txt")
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

    fun getDownloadedModels(): List<VoiceModel> {
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
            return Result.failure(DownloadException("Network error: ${e.message ?: e.javaClass.simpleName}", e))
        }

        response.use { r ->
            if (!r.isSuccessful) {
                return Result.failure(
                    DownloadException("Download failed: HTTP ${r.code} for $url")
                )
            }
            val body = r.body
                ?: return Result.failure(DownloadException("Empty response body for $url"))

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
                return Result.failure(DownloadException("I/O error: ${e.message ?: e.javaClass.simpleName}", e))
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

    companion object {
        private const val TAG = "OnnxModelDownloader"
        private const val MIN_ONNX_BYTES = 1_000_000L
        private const val PREFIX_BYTES = 512
    }
}

class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
