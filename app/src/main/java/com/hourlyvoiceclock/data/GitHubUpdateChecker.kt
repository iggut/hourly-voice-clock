package com.hourlyvoiceclock.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Production implementation of [UpdateChecker] that queries the GitHub
 * releases API for the latest APK.
 */
class GitHubUpdateChecker : UpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun checkForUpdate(currentVersion: String): Result<UpdateChecker.UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "HourlyVoiceClock-Updater")
                .build()

            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }

                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resume(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val responseCode = it.code
                            if (responseCode == 200) {
                                try {
                                    val responseBody = it.body?.string() ?: ""
                                    val json = JSONObject(responseBody)
                                    val latestVersion = json.optString("tag_name", "").trim()
                                    val htmlUrl = json.optString("html_url", "").trim()
                                    val body = json.optString("body", "").trim()

                                    // Extract apk download url if available, fallback to html url
                                    var downloadUrl = htmlUrl

                                    // Fast path: avoid parsing every object in the JSON array to save memory/CPU
                                    // GitHub API usually returns browser_download_url right after name
                                    val assetsArrayStr = json.optString("assets", "")
                                    if (assetsArrayStr.isNotEmpty()) {
                                        val apkIndex = assetsArrayStr.indexOf(".apk\"")
                                        if (apkIndex != -1) {
                                            val urlKeyIndex = assetsArrayStr.indexOf("\"browser_download_url\":\"", apkIndex)
                                            if (urlKeyIndex != -1) {
                                                // 24 is the length of "browser_download_url":"
                                                val start = urlKeyIndex + 24
                                                val end = assetsArrayStr.indexOf("\"", start)
                                                if (end != -1) {
                                                    downloadUrl = assetsArrayStr.substring(start, end)
                                                }
                                            }
                                        }
                                    }

                                    // Fallback to strict JSON parsing if fast path failed
                                    if (downloadUrl == htmlUrl) {
                                        json.optJSONArray("assets")?.let { assets ->
                                            val len = assets.length()
                                            for (i in 0 until len) {
                                                val asset = assets.optJSONObject(i) ?: continue
                                                val name = asset.optString("name", "")
                                                if (name.endsWith(".apk")) {
                                                    downloadUrl = asset.optString("browser_download_url", htmlUrl)
                                                    break
                                                }
                                            }
                                        }
                                    }

                                    val cleanCurrent = cleanVersion(currentVersion)
                                    val cleanLatest = cleanVersion(latestVersion)
                                    val updateAvailable = isNewerVersion(cleanCurrent, cleanLatest)

                                    continuation.resume(Result.success(
                                        UpdateChecker.UpdateInfo(
                                            isUpdateAvailable = updateAvailable,
                                            latestVersion = latestVersion,
                                            downloadUrl = downloadUrl,
                                            releaseNotes = body
                                        )
                                    ))
                                } catch (e: Exception) {
                                    continuation.resume(Result.failure(e))
                                }
                            } else if (responseCode == 404) {
                                continuation.resume(Result.success(
                                    UpdateChecker.UpdateInfo(
                                        isUpdateAvailable = false,
                                        latestVersion = "",
                                        downloadUrl = ""
                                    )
                                ))
                            } else {
                                continuation.resume(Result.failure(Exception("HTTP error $responseCode")))
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/iggut/hourly-voice-clock/releases/latest"

        fun cleanVersion(version: String): String {
            return version.trim().lowercase().removePrefix("v")
        }

        fun isNewerVersion(current: String, latest: String): Boolean {
            if (current.isBlank() || latest.isBlank()) return false
            val currentParts = parseVersion(current)
            val latestParts = parseVersion(latest)
            if (currentParts.numbers.isEmpty() || latestParts.numbers.isEmpty()) return false

            val count = maxOf(currentParts.numbers.size, latestParts.numbers.size)
            for (index in 0 until count) {
                val currentNumber = currentParts.numbers.getOrElse(index) { 0 }
                val latestNumber = latestParts.numbers.getOrElse(index) { 0 }
                if (latestNumber > currentNumber) return true
                if (currentNumber > latestNumber) return false
            }
            // Same numbers: 0.4.36 is newer than 0.4.36-alpha.
            return currentParts.preRelease != null && latestParts.preRelease == null
        }

        private data class ParsedVersion(val numbers: List<Int>, val preRelease: String?)

        private fun parseVersion(raw: String): ParsedVersion {
            val cleaned = cleanVersion(raw)
            val numbers = mutableListOf<Int>()
            var preRelease: String? = null
            for (piece in cleaned.split('.')) {
                val digits = piece.takeWhile { it.isDigit() }
                if (digits.isNotEmpty()) {
                    numbers += digits.toInt()
                }
                val rest = piece.drop(digits.length).trimStart('-', '+')
                if (rest.isNotEmpty() && preRelease == null) {
                    preRelease = rest
                }
            }
            return ParsedVersion(numbers, preRelease)
        }
    }
}
