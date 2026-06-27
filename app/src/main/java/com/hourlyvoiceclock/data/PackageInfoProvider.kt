package com.hourlyvoiceclock.data

import android.content.Context
import android.content.pm.PackageManager

/**
 * Adapter over [PackageManager] that exposes the app's own version name.
 *
 * Isolating this in a small interface makes update-check code testable
 * without Robolectric and removes the direct framework dependency from
 * ViewModels.
 */
interface PackageInfoProvider {
    fun versionName(): String
}

class AndroidPackageInfoProvider(context: Context) : PackageInfoProvider {

    private val appContext = context.applicationContext

    override fun versionName(): String {
        return try {
            val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            pInfo.versionName ?: DEFAULT_VERSION
        } catch (_: Exception) {
            DEFAULT_VERSION
        }
    }

    companion object {
        private const val DEFAULT_VERSION = "0.1"
    }
}
