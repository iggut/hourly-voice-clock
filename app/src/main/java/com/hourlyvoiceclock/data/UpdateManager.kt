package com.hourlyvoiceclock.data

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Manages the app-update lifecycle: check GitHub releases, download APK,
 * verify signature, and install.
 *
 * All methods are side-effecting and launch their own coroutine work.
 * Observability is through [status].
 */
interface UpdateManager {
    val status: StateFlow<UpdateStatus>

    fun checkForUpdate(currentVersion: String, isManual: Boolean = false)
    fun downloadAndInstall(downloadUrl: String, cacheDir: File)
    fun cancelDownload()
    fun installApk(localPath: String)
    fun cleanupAfterInstall(localPath: String?)
    fun dismissUpdateDialog()
}
