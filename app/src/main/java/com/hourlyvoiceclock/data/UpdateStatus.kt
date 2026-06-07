package com.hourlyvoiceclock.data

/**
 * States for the in-app update flow (check → download → verify → install).
 */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    ) : UpdateStatus
    data class Downloading(
        val progress: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateStatus
    data class InstallReady(val localApkPath: String) : UpdateStatus
    data class Installing(val progress: Int) : UpdateStatus
    data object InstallComplete : UpdateStatus
    data class InstallFailed(val error: String) : UpdateStatus
    data object UpToDate : UpdateStatus
    data object NoRelease : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}
