package com.hourlyvoiceclock.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L
)

class UpdateDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var currentCall: okhttp3.Call? = null
    private var downloadFile: File? = null

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    @Volatile
    private var isCancelled = false

    suspend fun downloadApk(url: String, cacheDir: File): Result<String> = withContext(Dispatchers.IO) {
        isCancelled = false
        _downloadProgress.value = DownloadProgress()

        val apkFileName = "update_${System.currentTimeMillis()}.apk"
        downloadFile = File(cacheDir, apkFileName)

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HourlyVoiceClock-Updater")
                .build()

            currentCall = client.newCall(request)
            val response = currentCall!!.execute()

            if (isCancelled) {
                currentCall = null
                cleanupPartialDownload()
                return@withContext Result.failure(Exception("Download cancelled"))
            }

            if (!response.isSuccessful) {
                currentCall = null
                cleanupPartialDownload()
                return@withContext Result.failure(Exception("HTTP error ${response.code}"))
            }

            val body = response.body ?: run {
                currentCall = null
                cleanupPartialDownload()
                return@withContext Result.failure(Exception("Empty response body"))
            }

            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L

            FileOutputStream(downloadFile).use { outputStream ->
                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled) {
                            currentCall = null
                            cleanupPartialDownload()
                            return@withContext Result.failure(Exception("Download cancelled"))
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((bytesDownloaded * 100) / totalBytes).toInt()
                        } else {
                            0
                        }

                        _downloadProgress.value = DownloadProgress(
                            progress = progress.coerceIn(0, 100),
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes
                        )
                    }
                }
            }

            currentCall = null

            if (isCancelled) {
                cleanupPartialDownload()
                return@withContext Result.failure(Exception("Download cancelled"))
            }

            val filePath = downloadFile?.absolutePath
            if (filePath != null) {
                Result.success(filePath)
            } else {
                Result.failure(Exception("Download failed - no file path"))
            }
        } catch (e: Exception) {
            currentCall = null
            cleanupPartialDownload()
            Result.failure(e)
        }
    }

    fun cancel() {
        isCancelled = true
        currentCall?.cancel()
        currentCall = null
    }

    private fun cleanupPartialDownload() {
        downloadFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        downloadFile = null
    }

    fun cleanupDownload(filePath: String?) {
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        _downloadProgress.value = DownloadProgress()
    }
}