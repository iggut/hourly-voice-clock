package com.hourlyvoiceclock.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUpdateManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Test
    fun `checkForUpdate emits UpdateAvailable when newer version exists`() {
        val checker = FakeUpdateChecker(
            result = Result.success(
                UpdateChecker.UpdateInfo(
                    isUpdateAvailable = true,
                    latestVersion = "v1.2.0",
                    downloadUrl = "https://example.com/app.apk"
                )
            )
        )
        val manager = createManager(updateChecker = checker)

        manager.checkForUpdate("1.1.0", isManual = false)

        val status = manager.status.value
        assertTrue(status is UpdateStatus.UpdateAvailable)
        assertEquals("v1.2.0", (status as UpdateStatus.UpdateAvailable).latestVersion)
    }

    @Test
    fun `checkForUpdate emits UpToDate when no newer version`() {
        val checker = FakeUpdateChecker(
            result = Result.success(
                UpdateChecker.UpdateInfo(
                    isUpdateAvailable = false,
                    latestVersion = "v1.1.0",
                    downloadUrl = ""
                )
            )
        )
        val manager = createManager(updateChecker = checker)

        manager.checkForUpdate("1.1.0", isManual = false)

        assertTrue(manager.status.value is UpdateStatus.UpToDate)
    }

    @Test
    fun `checkForUpdate shows error toast on manual failure`() {
        val checker = FakeUpdateChecker(result = Result.failure(Exception("network down")))
        val uiDelegate = FakeUpdateUiDelegate()
        val manager = createManager(updateChecker = checker, uiDelegate = uiDelegate)

        manager.checkForUpdate("1.0.0", isManual = true)

        assertTrue(manager.status.value is UpdateStatus.Error)
        assertTrue(uiDelegate.errorShown)
        assertEquals("network down", uiDelegate.lastErrorMessage)
    }

    @Test
    fun `downloadAndInstall emits InstallReady on success`() {
        val downloader = FakeUpdateDownloader(Result.success("/tmp/app.apk"))
        val manager = createManager(downloader = downloader)

        manager.downloadAndInstall("https://example.com/app.apk", File("/tmp"))

        assertTrue(manager.status.value is UpdateStatus.InstallReady)
        assertEquals("/tmp/app.apk", (manager.status.value as UpdateStatus.InstallReady).localApkPath)
    }

    @Test
    fun `downloadAndInstall emits InstallFailed on error`() {
        val downloader = FakeUpdateDownloader(Result.failure(Exception("disk full")))
        val manager = createManager(downloader = downloader)

        manager.downloadAndInstall("https://example.com/app.apk", File("/tmp"))

        assertTrue(manager.status.value is UpdateStatus.InstallFailed)
    }

    @Test
    fun `installApk launches intent when signatures match`() {
        val verifier = FakeSignatureVerifier(SignatureVerifier.VerifyResult.SignaturesMatch)
        val uiDelegate = FakeUpdateUiDelegate()
        val manager = createManager(signatureVerifier = verifier, uiDelegate = uiDelegate)

        manager.installApk("/tmp/app.apk")

        assertTrue(uiDelegate.installLaunched)
        assertEquals("/tmp/app.apk", uiDelegate.lastLocalApkPath)
        assertTrue(manager.status.value is UpdateStatus.InstallComplete)
    }

    @Test
    fun `installApk fails when signatures mismatch`() {
        val verifier = FakeSignatureVerifier(
            SignatureVerifier.VerifyResult.SignatureMismatch(
                localFingerprint = "A1",
                apkFingerprint = "B2",
                message = "mismatch"
            )
        )
        val manager = createManager(signatureVerifier = verifier)

        manager.installApk("/tmp/app.apk")

        assertTrue(manager.status.value is UpdateStatus.InstallFailed)
    }

    @Test
    fun `dismissUpdateDialog cleans up install-ready file`() {
        val downloader = FakeUpdateDownloader(Result.success("/tmp/app.apk"))
        val manager = createManager(downloader = downloader)
        manager.downloadAndInstall("url", File("/tmp"))
        assertTrue(manager.status.value is UpdateStatus.InstallReady)

        manager.dismissUpdateDialog()

        assertTrue(manager.status.value is UpdateStatus.Idle)
        assertTrue(downloader.cleanedUp)
    }

    private fun createManager(
        updateChecker: UpdateChecker = FakeUpdateChecker(Result.success(UpdateChecker.UpdateInfo(false, "", ""))),
        downloader: UpdateDownloader = FakeUpdateDownloader(Result.success("/tmp/app.apk")),
        signatureVerifier: SignatureVerifier = FakeSignatureVerifier(SignatureVerifier.VerifyResult.SignaturesMatch),
        uiDelegate: UpdateUiDelegate = FakeUpdateUiDelegate()
    ): DefaultUpdateManager {
        return DefaultUpdateManager(
            scope = testScope,
            updateChecker = updateChecker,
            downloader = downloader,
            signatureVerifier = signatureVerifier,
            uiDelegate = uiDelegate
        )
    }
}

private class FakeUpdateChecker(private val result: Result<UpdateChecker.UpdateInfo>) : UpdateChecker {
    override suspend fun checkForUpdate(currentVersion: String): Result<UpdateChecker.UpdateInfo> = result
}

private class FakeUpdateDownloader(
    private val result: Result<String>
) : UpdateDownloader {

    override val downloadProgress = MutableStateFlow(DownloadProgress())
    var cleanedUp = false

    override suspend fun downloadApk(url: String, cacheDir: File): Result<String> = result

    override fun cancel() {}

    override fun cleanupDownload(filePath: String?) {
        cleanedUp = true
    }
}

private class FakeSignatureVerifier(
    private val result: SignatureVerifier.VerifyResult
) : SignatureVerifier {
    override fun verifyUpdateCompatibility(localApkPath: String): SignatureVerifier.VerifyResult = result
}

private class FakeUpdateUiDelegate : UpdateUiDelegate {
    var errorShown = false
    var lastErrorMessage: String? = null
    var installLaunched = false
    var lastLocalApkPath: String? = null

    override fun showUpdateCheckError(message: String?) {
        errorShown = true
        lastErrorMessage = message
    }

    override fun launchInstallIntent(localApkPath: String) {
        installLaunched = true
        lastLocalApkPath = localApkPath
    }
}
