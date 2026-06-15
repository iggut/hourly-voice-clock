package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads Piper voice distributions (the .onnx model + its sibling
 * .onnx.json audio/phoenome-id config) from the HuggingFace
 * `rhasspy/piper-voices` mirror and verifies the response.
 *
 * The "already downloaded" check is **existence-based** (file present
 * and non-empty), not size-based, so a partial download on the first
 * try will be re-fetched without false-positive short-circuits.
 *
 * Each [downloadModel] call is wrapped in a coroutine bound to
 * [Dispatchers.IO]. Failures are returned as a [Result] and also
 * re-thrown as a typed [DownloadException] so the caller can render
 * a human-readable error in the UI without parsing log lines.
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

        // Existence-based early-out: both files present and non-empty.
        if (isModelDownloaded(model)) {
            Log.d(TAG, "Model ${model.id} already downloaded")
            return@withContext Result.success(onnxFile)
        }

        // Clean any partial state from a previous attempt.
        if (modelDir.exists()) {
            onnxFile.delete()
            jsonFile.delete()
        } else {
            modelDir.mkdirs()
        }

        try {
            val onnxResult = downloadOne(model.onnxDownloadUrl, onnxFile) { fraction ->
                // The .onnx file is ~99% of total bytes; map it to 0..0.95.
                onProgress(fraction * 0.95f)
            }
            if (onnxResult.isFailure) {
                onnxFile.delete()
                return@withContext Result.failure(onnxResult.exceptionOrNull()!!)
            }

            val jsonResult = downloadOne(model.onnxJsonDownloadUrl, jsonFile) { fraction ->
                onProgress(0.95f + fraction * 0.05f)
            }
            if (jsonResult.isFailure) {
                onnxFile.delete()
                jsonFile.delete()
                return@withContext Result.failure(jsonResult.exceptionOrNull()!!)
            }

            onProgress(1f)
            Log.d(
                TAG,
                "Downloaded model ${model.id} (${onnxFile.length()} + ${jsonFile.length()} bytes)"
            )
            Result.success(onnxFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model ${model.id}", e)
            // Best-effort cleanup of partial state.
            onnxFile.delete()
            jsonFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Returns true if the model is fully present on disk (both .onnx and
     * .onnx.json, each non-empty). Exposed as a suspend function so it
     * can be called from a coroutine that already has a dispatcher
     * (avoids the implicit main-thread File I/O of the original).
     */
    suspend fun isModelDownloadedAsync(model: VoiceModel): Boolean = withContext(Dispatchers.IO) {
        isModelDownloaded(model)
    }

    fun isModelDownloaded(model: VoiceModel): Boolean {
        val onnx = File(modelsDir, "${model.id}/${model.onnxFileName}")
        val json = File(modelsDir, "${model.id}/${model.onnxJsonFileName}")
        return onnx.exists() && onnx.length() > 0 && json.exists() && json.length() > 0
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

    companion object {
        private const val TAG = "OnnxModelDownloader"
    }
}

class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
