package com.hourlyvoiceclock.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/iggut/hourly-voice-clock/releases/latest"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String = ""
    )

    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "HourlyVoiceClock-Updater")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                val latestVersion = json.optString("tag_name", "").trim()
                val htmlUrl = json.optString("html_url", "").trim()
                val body = json.optString("body", "").trim()

                // Extract apk download url if available, fallback to html url
                var downloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                val cleanCurrent = cleanVersion(currentVersion)
                val cleanLatest = cleanVersion(latestVersion)
                val updateAvailable = isNewerVersion(cleanCurrent, cleanLatest)

                Result.success(
                    UpdateInfo(
                        isUpdateAvailable = updateAvailable,
                        latestVersion = latestVersion,
                        downloadUrl = downloadUrl,
                        releaseNotes = body
                    )
                )
            } else {
                Result.failure(Exception("HTTP error $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun cleanVersion(version: String): String {
        return version.trim().lowercase().removePrefix("v")
    }

    internal fun isNewerVersion(current: String, latest: String): Boolean {
        if (current.isBlank() || latest.isBlank()) return false
        if (current == latest) return false

        val currentParts = current.split(".")
        val latestParts = latest.split(".")
        val minSize = minOf(currentParts.size, latestParts.size)

        for (i in 0 until minSize) {
            val currNum = getNumericPart(currentParts[i])
            val lateNum = getNumericPart(latestParts[i])
            if (lateNum > currNum) return true
            if (currNum > lateNum) return false
        }
        return latestParts.size > currentParts.size
    }

    private fun getNumericPart(part: String): Int {
        val digits = part.takeWhile { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}
