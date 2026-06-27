package com.hourlyvoiceclock.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Port for Android-specific UI side effects triggered by the update flow.
 *
 * Isolating Toast and install-intent launching from [DefaultUpdateManager]
 * keeps the manager focused on state transitions and makes it unit-testable
 * with a fake delegate.
 */
interface UpdateUiDelegate {
    fun showUpdateCheckError(message: String?)
    fun launchInstallIntent(localApkPath: String)
}

class AndroidUpdateUiDelegate(private val context: Context) : UpdateUiDelegate {

    private val appContext = context.applicationContext

    override fun showUpdateCheckError(message: String?) {
        Toast.makeText(
            appContext,
            "Update check failed: ${message ?: "Unknown error"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun launchInstallIntent(localApkPath: String) {
        val file = File(localApkPath)
        if (!file.exists()) throw Exception("APK file not found")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }

        appContext.startActivity(intent)
    }
}
