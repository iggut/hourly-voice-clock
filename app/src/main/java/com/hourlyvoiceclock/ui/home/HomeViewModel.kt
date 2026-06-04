package com.hourlyvoiceclock.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.HourlyVoiceClockApp
import com.hourlyvoiceclock.announcer.QuietHoursPolicy
import com.hourlyvoiceclock.announcer.TimeAnnouncer
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.data.SignatureVerifier
import com.hourlyvoiceclock.data.UpdateDownloader
import com.hourlyvoiceclock.scheduler.AnnouncementScheduler
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpdateAvailable(val latestVersion: String, val downloadUrl: String, val releaseNotes: String) : UpdateStatus
    data class Downloading(val progress: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateStatus
    data class InstallReady(val localApkPath: String) : UpdateStatus
    data class Installing(val progress: Int) : UpdateStatus
    object InstallComplete : UpdateStatus
    data class InstallFailed(val error: String) : UpdateStatus
    object UpToDate : UpdateStatus
    object NoRelease : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val scheduler = AnnouncementScheduler(application)
    private val ttsRepo = TtsVoiceRepository((application as HourlyVoiceClockApp).ttsEngine)
    private val announcer = TimeAnnouncer(application, ttsRepo)
    private val updateDownloader = UpdateDownloader()

    val appSettings: StateFlow<com.hourlyvoiceclock.data.AppSettings> = settingsRepo.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.hourlyvoiceclock.data.AppSettings()
        )

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _nextAnnouncement = MutableStateFlow("")
    val nextAnnouncement: StateFlow<String> = _nextAnnouncement.asStateFlow()

    private val _quietHoursActive = MutableStateFlow(false)
    val quietHoursActive: StateFlow<Boolean> = _quietHoursActive.asStateFlow()

    private val _hourlyEnabled = MutableStateFlow(false)
    val hourlyEnabled: StateFlow<Boolean> = _hourlyEnabled.asStateFlow()

    private val _canSpeakNow = MutableStateFlow(true)
    val canSpeakNow: StateFlow<Boolean> = _canSpeakNow.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private var startupAutoCheckDone = false
    private var cachedDate: java.time.LocalDate? = null
    private var cachedNextAnnouncementTime: LocalDateTime? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                updateTime()
                if (_hourlyEnabled.value) {
                    updateNextAnnouncement(true)
                }
                delay(1000)
            }
        }
        viewModelScope.launch {
            var previousAutoUpdateEnabled: Boolean? = null
            settingsRepo.settings.collect { settings ->
                _hourlyEnabled.value = settings.hourlyAnnouncementsEnabled
                updateQuietStatus(settings)
                val reEnabled =
                    previousAutoUpdateEnabled == false && settings.autoUpdateEnabled
                previousAutoUpdateEnabled = settings.autoUpdateEnabled
                if (settings.autoUpdateEnabled && (!startupAutoCheckDone || reEnabled)) {
                    startupAutoCheckDone = true
                    checkForUpdates(isManual = false)
                }
            }
        }
        viewModelScope.launch {
            val initOk = ttsRepo.initialize()
            if (!initOk) {
                Toast.makeText(getApplication(), "Text-to-Speech initialization failed. Check your TTS engine in system settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTime() {
        val now = LocalDateTime.now()
        // Format time every second
        _currentTime.value = now.format(TIME_FORMATTER)

        // Optimize: Only format date when the day changes
        val today = now.toLocalDate()
        if (cachedDate != today) {
            cachedDate = today
            _currentDate.value = now.format(DATE_FORMATTER)
        }
    }

    private fun updateNextAnnouncement(enabled: Boolean) {
        if (!enabled) {
            _nextAnnouncement.value = ""
            cachedNextAnnouncementTime = null
            return
        }
        val next = AnnouncementScheduler.getNextTopOfHour()
        // Optimize: Only format next announcement when the target hour changes
        if (cachedNextAnnouncementTime != next) {
            cachedNextAnnouncementTime = next
            _nextAnnouncement.value = next.format(NEXT_ANNOUNCEMENT_FORMATTER)
        }
    }

    private fun updateQuietStatus(settings: com.hourlyvoiceclock.data.AppSettings) {
        val now = java.time.LocalTime.now()
        val inQuiet = QuietHoursPolicy.isQuietTime(
            now,
            settings.quietHoursEnabled,
            settings.quietHoursStart,
            settings.quietHoursEnd
        )
        _quietHoursActive.value = inQuiet
        _canSpeakNow.value = QuietHoursPolicy.canAnnounceManually(
            now,
            settings.quietHoursEnabled,
            settings.quietHoursStart,
            settings.quietHoursEnd,
            settings.allowManualDuringQuiet
        )
    }

    fun toggleHourly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setHourlyAnnouncements(enabled)
            val settings = settingsRepo.settings.first()
            if (enabled) {
                scheduler.scheduleNextHour(settings.exactAlarmsEnabled)
            } else {
                scheduler.cancelHourlyAlarms()
            }
        }
    }

    fun announceNow(includeDate: Boolean = false) {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            announcer.announce(settings, force = true, includeDate = includeDate)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAutoUpdateEnabled(enabled)
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val currentVersion = try {
                val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                pInfo.versionName ?: "0.1"
            } catch (e: Exception) {
                "0.1"
            }

            com.hourlyvoiceclock.data.UpdateChecker.checkForUpdate(currentVersion)
                .onSuccess { info ->
                    if (info.isUpdateAvailable) {
                        _updateStatus.value = UpdateStatus.UpdateAvailable(
                            latestVersion = info.latestVersion,
                            downloadUrl = info.downloadUrl,
                            releaseNotes = info.releaseNotes
                        )
                    } else if (info.latestVersion.isBlank()) {
                        _updateStatus.value = UpdateStatus.NoRelease
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                    }
                }
                .onFailure { error ->
                    _updateStatus.value = UpdateStatus.Error(error.localizedMessage ?: "Unknown error")
                    if (isManual) {
                        Toast.makeText(
                            getApplication(),
                            "Update check failed: ${error.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    fun downloadAndInstall(downloadUrl: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _updateStatus.value = UpdateStatus.Downloading(0, 0, 0)

            // Start progress collection BEFORE download
            val progressJob = launch {
                updateDownloader.downloadProgress.collect { progress ->
                    val currentStatus = _updateStatus.value
                    if (currentStatus is UpdateStatus.Downloading) {
                        _updateStatus.value = UpdateStatus.Downloading(
                            progress = progress.progress,
                            bytesDownloaded = progress.bytesDownloaded,
                            totalBytes = progress.totalBytes
                        )
                    }
                }
            }

            updateDownloader.downloadApk(downloadUrl, context.cacheDir)
                .onSuccess { filePath ->
                    progressJob.cancel()
                    _updateStatus.value = UpdateStatus.InstallReady(filePath)
                }
                .onFailure { error ->
                    progressJob.cancel()
                    _updateStatus.value = UpdateStatus.InstallFailed(error.localizedMessage ?: "Download failed")
                }
        }
    }

    fun cancelDownload() {
        updateDownloader.cancel()
        val currentStatus = _updateStatus.value
        if (currentStatus is UpdateStatus.Downloading) {
            _updateStatus.value = UpdateStatus.Idle
        }
    }

    fun installApk(localPath: String) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Installing(0)
            val context = getApplication<Application>()
            try {
                // Pre-installation signature verification
                val verificationResult = SignatureVerifier.verifyUpdateCompatibility(context, localPath)
                when (verificationResult) {
                    is SignatureVerifier.VerifyResult.SignatureMismatch -> {
                        _updateStatus.value = UpdateStatus.InstallFailed(
                            "Cannot install update: Signature mismatch. " +
                            "This app was installed via a different signing key. " +
                            "Please uninstall the current app and reinstall from the APK file."
                        )
                        return@launch
                    }
                    is SignatureVerifier.VerifyResult.Error -> {
                        _updateStatus.value = UpdateStatus.InstallFailed(verificationResult.message)
                        return@launch
                    }
                    else -> {
                        // Signatures match or other OK result - proceed with installation
                    }
                }

                installApkInternal(context, localPath)
                // Installation was initiated successfully
                // The app will restart or the user will return to the app
                _updateStatus.value = UpdateStatus.InstallComplete
            } catch (e: Exception) {
                val message = e.localizedMessage ?: "Installation failed"
                // Check if this is a signature-related error
                if (message.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES", ignoreCase = true) ||
                    message.contains("signature", ignoreCase = true)) {
                    _updateStatus.value = UpdateStatus.InstallFailed(
                        "Installation failed due to signature mismatch. " +
                        "Please uninstall the current app and try installing again."
                    )
                } else {
                    _updateStatus.value = UpdateStatus.InstallFailed(message)
                }
            }
        }
    }

    private fun installApkInternal(context: Context, apkPath: String) {
        val file = java.io.File(apkPath)
        if (!file.exists()) {
            throw Exception("APK file not found")
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Try to use FileProvider if available
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Fallback to file:// URI
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }

        context.startActivity(intent)
    }

    fun cleanupAfterInstall(localPath: String?) {
        if (localPath != null) {
            updateDownloader.cleanupDownload(localPath)
        }
    }

    fun dismissUpdateDialog() {
        val currentStatus = _updateStatus.value
        when (currentStatus) {
            is UpdateStatus.Downloading -> cancelDownload()
            is UpdateStatus.InstallReady, is UpdateStatus.InstallFailed -> {
                val path = if (currentStatus is UpdateStatus.InstallReady) currentStatus.localApkPath else null
                cleanupAfterInstall(path)
            }
            else -> {}
        }
        _updateStatus.value = UpdateStatus.Idle
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm:ss a")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        private val NEXT_ANNOUNCEMENT_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
    }
}
