package com.hourlyvoiceclock.data

import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class DownloadProgress(
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L
)

/**
 * Port for downloading an APK to a local file.
 */
interface UpdateDownloader {

    val downloadProgress: StateFlow<DownloadProgress>

    suspend fun downloadApk(url: String, cacheDir: File): Result<String>

    fun cancel()

    fun cleanupDownload(filePath: String?)
}
