package com.hourlyvoiceclock.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Orchestrates the app-update lifecycle: check GitHub releases, download APK,
 * verify signatures, and trigger the installer.
 *
 * All Android-specific side effects (Toast, install Intent) are delegated to
 * [UpdateUiDelegate]; network I/O is delegated to [UpdateChecker] and
 * [UpdateDownloader]; signature checks go through [SignatureVerifier]. This
 * keeps the manager focused on state transitions and makes it testable with
 * fakes.
 */
class DefaultUpdateManager(
    private val scope: CoroutineScope,
    private val updateChecker: UpdateChecker,
    private val downloader: UpdateDownloader,
    private val signatureVerifier: SignatureVerifier,
    private val uiDelegate: UpdateUiDelegate
) : UpdateManager {

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    override val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    override fun checkForUpdate(currentVersion: String, isManual: Boolean) {
        scope.launch {
            _status.value = UpdateStatus.Checking

            updateChecker.checkForUpdate(currentVersion)
                .onSuccess { info ->
                    _status.value = when {
                        info.isUpdateAvailable -> UpdateStatus.UpdateAvailable(
                            latestVersion = info.latestVersion,
                            downloadUrl = info.downloadUrl,
                            releaseNotes = info.releaseNotes
                        )
                        info.latestVersion.isBlank() -> UpdateStatus.NoRelease
                        else -> UpdateStatus.UpToDate
                    }
                }
                .onFailure { error ->
                    _status.value = UpdateStatus.Error(error.localizedMessage ?: "Unknown error")
                    if (isManual) {
                        uiDelegate.showUpdateCheckError(error.localizedMessage)
                    }
                }
        }
    }

    override fun downloadAndInstall(downloadUrl: String, cacheDir: File) {
        scope.launch {
            _status.value = UpdateStatus.Downloading(0, 0, 0)

            val progressJob = scope.launch {
                downloader.downloadProgress.collect { progress ->
                    val current = _status.value
                    if (current is UpdateStatus.Downloading) {
                        _status.value = UpdateStatus.Downloading(
                            progress = progress.progress,
                            bytesDownloaded = progress.bytesDownloaded,
                            totalBytes = progress.totalBytes
                        )
                    }
                }
            }

            downloader.downloadApk(downloadUrl, cacheDir)
                .onSuccess { filePath ->
                    progressJob.cancel()
                    _status.value = UpdateStatus.InstallReady(filePath)
                }
                .onFailure { error ->
                    progressJob.cancel()
                    _status.value = UpdateStatus.InstallFailed(error.localizedMessage ?: "Download failed")
                }
        }
    }

    override fun cancelDownload() {
        downloader.cancel()
        val current = _status.value
        if (current is UpdateStatus.Downloading) {
            _status.value = UpdateStatus.Idle
        }
    }

    override fun installApk(localPath: String) {
        scope.launch {
            _status.value = UpdateStatus.Installing(0)
            try {
                val result = signatureVerifier.verifyUpdateCompatibility(localPath)
                when (result) {
                    is SignatureVerifier.VerifyResult.SignatureMismatch -> {
                        _status.value = UpdateStatus.InstallFailed(
                            "Cannot install update: Signature mismatch. " +
                            "This app was installed via a different signing key. " +
                            "Please uninstall the current app and reinstall from the APK file."
                        )
                        return@launch
                    }
                    is SignatureVerifier.VerifyResult.Error -> {
                        _status.value = UpdateStatus.InstallFailed(result.message)
                        return@launch
                    }
                    else -> { /* proceed */ }
                }

                uiDelegate.launchInstallIntent(localPath)
                _status.value = UpdateStatus.InstallComplete
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Installation failed"
                _status.value = if (
                    msg.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES", ignoreCase = true) ||
                    msg.contains("signature", ignoreCase = true)
                ) {
                    UpdateStatus.InstallFailed(
                        "Installation failed due to signature mismatch. " +
                        "Please uninstall the current app and try installing again."
                    )
                } else {
                    UpdateStatus.InstallFailed(msg)
                }
            }
        }
    }

    override fun cleanupAfterInstall(localPath: String?) {
        if (localPath != null) {
            downloader.cleanupDownload(localPath)
        }
    }

    override fun dismissUpdateDialog() {
        val current = _status.value
        when (current) {
            is UpdateStatus.Downloading -> cancelDownload()
            is UpdateStatus.InstallReady, is UpdateStatus.InstallFailed -> {
                val path = if (current is UpdateStatus.InstallReady) current.localApkPath else null
                cleanupAfterInstall(path)
            }
            else -> {}
        }
        _status.value = UpdateStatus.Idle
    }
}
