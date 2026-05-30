package com.hourlyvoiceclock.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Applies forward-compatible migrations to persisted preferences so installs can
 * upgrade from any prior app version without crashing or retaining invalid state.
 */
object SettingsMigration {

    const val CURRENT_SCHEMA_VERSION = 1

    private val KEY_SCHEMA_VERSION = intPreferencesKey("settings_schema_version")
    private val KEY_PITCH = floatPreferencesKey("pitch")
    private val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
    private val KEY_SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
    private val KEY_SELECTED_LOCALE = stringPreferencesKey("selected_locale")
    private val KEY_SELECTED_TTS_ENGINE_PACKAGE = stringPreferencesKey("selected_tts_engine_package")
    private val KEY_SELECTED_VOICE_PRESET_ID = stringPreferencesKey("selected_voice_preset_id")

    fun migrateInPlace(prefs: MutablePreferences) {
        var version = prefs[KEY_SCHEMA_VERSION] ?: 0

        if (version < 1) {
            migrateToV1(prefs)
            version = 1
        }

        prefs[KEY_SCHEMA_VERSION] = version
    }

    private fun migrateToV1(prefs: MutablePreferences) {
        prefs[KEY_PITCH]?.let { prefs[KEY_PITCH] = it.coerceIn(0.5f, 2.0f) }
        prefs[KEY_SPEECH_RATE]?.let { prefs[KEY_SPEECH_RATE] = it.coerceIn(0.5f, 2.0f) }

        sanitizeOptionalString(prefs, KEY_SELECTED_VOICE_NAME)
        sanitizeOptionalString(prefs, KEY_SELECTED_LOCALE)
        sanitizeOptionalString(prefs, KEY_SELECTED_TTS_ENGINE_PACKAGE)
        sanitizeOptionalString(prefs, KEY_SELECTED_VOICE_PRESET_ID)
    }

    private fun sanitizeOptionalString(prefs: MutablePreferences, key: Preferences.Key<String>) {
        val value = prefs[key] ?: return
        if (value.isBlank()) {
            prefs.remove(key)
        }
    }
}
