package com.hourlyvoiceclock.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/iggut/hourly-voice-clock/releases/latest"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String = ""
    )

    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
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

                                    continuation.resume(Result.success(
                                        UpdateInfo(
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
                                    UpdateInfo(
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

    internal fun cleanVersion(version: String): String {
        return version.trim().lowercase().removePrefix("v")
    }

    internal fun isNewerVersion(current: String, latest: String): Boolean {
        if (current.isBlank() || latest.isBlank()) return false
        if (current == latest) return false

        var currIdx = 0
        var lateIdx = 0
        val currLen = current.length
        val lateLen = latest.length

        while (currIdx < currLen && lateIdx < lateLen) {
            var currNum = 0
            var lateNum = 0

            var currParsingDigits = true
            while (currIdx < currLen) {
                val c = current[currIdx++]
                if (c == '.') break
                if (currParsingDigits) {
                    if (c.isDigit()) {
                        currNum = currNum * 10 + (c - '0')
                    } else {
                        currParsingDigits = false
                    }
                }
            }

            var lateParsingDigits = true
            while (lateIdx < lateLen) {
                val c = latest[lateIdx++]
                if (c == '.') break
                if (lateParsingDigits) {
                    if (c.isDigit()) {
                        lateNum = lateNum * 10 + (c - '0')
                    } else {
                        lateParsingDigits = false
                    }
                }
            }

            if (lateNum > currNum) return true
            if (currNum > lateNum) return false
        }

        var currentPartsCount = 1
        for (i in 0 until currLen) {
            if (current[i] == '.') currentPartsCount++
        }
        var latestPartsCount = 1
        for (i in 0 until lateLen) {
            if (latest[i] == '.') latestPartsCount++
        }

        return latestPartsCount > currentPartsCount
    }
}
