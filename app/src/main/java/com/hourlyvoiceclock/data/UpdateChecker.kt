package com.hourlyvoiceclock.data

/**
 * Port for checking whether a newer app release is available.
 */
interface UpdateChecker {

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String = ""
    )

    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo>
}
