package com.hourlyvoiceclock.tts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.hourlyvoiceclock.data.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Resolves the TTS engine package the app should use at startup.
 *
 * Reads the user's saved choice from [SettingsRepository], validates that
 * the package is still installed, and — if the package is no longer
 * present — clears the stale value in the repository so the user is not
 * silently stuck on an uninstalled engine.
 *
 * The previous behaviour lived inline inside [AndroidTtsEngine.initialize]
 * and reached out to a second [SettingsRepository] instance, bypassing
 * the DI graph. Pulling the policy out of the engine makes it
 * unit-testable and stops the engine from depending on the repository.
 */
class TtsEngineSelector(
    private val settings: SettingsRepository,
    private val packageProbe: TtsPackageProbe
) {
    /**
     * Returns the engine package to use: the user's saved choice if it
     * is still installed, otherwise `null` (and the saved value is
     * cleared from the repository so the next read returns `null`).
     */
    suspend fun select(): String? {
        val saved = settings.settings.first().selectedTtsEnginePackage
        if (saved.isNullOrBlank()) return null
        if (packageProbe.isInstalled(saved)) return saved
        Log.w(TAG, "Saved TTS engine $saved is not installed; clearing saved value")
        try {
            settings.setSelectedTtsEnginePackage(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear stale TTS engine selection", e)
        }
        return null
    }

    companion object {
        private const val TAG = "TtsEngineSelector"
    }
}

/**
 * Indirection over [PackageManager] so the selector can be unit-tested
 * without a real device or Robolectric.
 */
interface TtsPackageProbe {
    fun isInstalled(packageName: String): Boolean
}

/** Production probe: queries the system for a TTS service. */
class AndroidTtsPackageProbe(private val context: Context) : TtsPackageProbe {
    override fun isInstalled(packageName: String): Boolean {
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val services = context.packageManager.queryIntentServices(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return services.any { it.serviceInfo.packageName == packageName }
    }
}
