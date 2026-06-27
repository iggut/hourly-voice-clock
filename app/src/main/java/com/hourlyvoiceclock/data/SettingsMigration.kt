package com.hourlyvoiceclock.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.intPreferencesKey

/**
 * Applies forward-compatible migrations to persisted preferences so installs can
 * upgrade from any prior app version without crashing or retaining invalid state.
 *
 * Migration keys are shared with [SettingsMapper] so renames cannot silently
 * drift out of sync between the current schema and the migration code.
 */
object SettingsMigration {

    const val CURRENT_SCHEMA_VERSION = 1

    private val KEY_SCHEMA_VERSION = intPreferencesKey("settings_schema_version")

    fun migrateInPlace(prefs: MutablePreferences) {
        var version = prefs[KEY_SCHEMA_VERSION] ?: 0

        if (version < 1) {
            migrateToV1(prefs)
            version = 1
        }

        prefs[KEY_SCHEMA_VERSION] = version
    }

    private fun migrateToV1(prefs: MutablePreferences) {
        with(SettingsMapper) {
            prefs[KEY_PITCH]?.let { prefs[KEY_PITCH] = it.coerceIn(0.5f, 2.0f) }
            prefs[KEY_SPEECH_RATE]?.let { prefs[KEY_SPEECH_RATE] = it.coerceIn(0.5f, 2.0f) }

            sanitizeOptionalString(prefs, KEY_SELECTED_VOICE_NAME)
            sanitizeOptionalString(prefs, KEY_SELECTED_LOCALE)
            sanitizeOptionalString(prefs, KEY_SELECTED_TTS_ENGINE_PACKAGE)
            sanitizeOptionalString(prefs, KEY_SELECTED_VOICE_PRESET_ID)
        }
    }

    private fun sanitizeOptionalString(prefs: MutablePreferences, key: androidx.datastore.preferences.core.Preferences.Key<String>) {
        val value = prefs[key] ?: return
        if (value.isBlank()) {
            prefs.remove(key)
        }
    }
}
