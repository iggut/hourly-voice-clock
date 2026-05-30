package com.hourlyvoiceclock

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hourlyvoiceclock.data.SettingsMigration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsMigrationTest {

    @Test
    fun `migrateToV1 clamps pitch and speech rate into valid range`() {
        val prefs = mutablePreferencesOf(
            floatPreferencesKey("pitch") to 9.5f,
            floatPreferencesKey("speech_rate") to 0.1f
        )

        SettingsMigration.migrateInPlace(prefs)

        assertEquals(2.0f, prefs[floatPreferencesKey("pitch")])
        assertEquals(0.5f, prefs[floatPreferencesKey("speech_rate")])
        assertEquals(
            SettingsMigration.CURRENT_SCHEMA_VERSION,
            prefs[intPreferencesKey("settings_schema_version")]
        )
    }

    @Test
    fun `migrateToV1 removes blank optional strings`() {
        val voiceKey = stringPreferencesKey("selected_voice_name")
        val engineKey = stringPreferencesKey("selected_tts_engine_package")
        val prefs = mutablePreferencesOf(
            voiceKey to "   ",
            engineKey to ""
        )

        SettingsMigration.migrateInPlace(prefs)

        assertNull(prefs[voiceKey])
        assertNull(prefs[engineKey])
    }
}
