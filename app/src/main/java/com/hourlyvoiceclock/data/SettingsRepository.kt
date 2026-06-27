package com.hourlyvoiceclock.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Public settings seam. Exposes the user's [AppSettings] as a [Flow] and
 * an atomic [update] transform for all writes.
 *
 * Callers read through [settings] and mutate through [update]; there are
 * no per-field setters. This keeps the interface small and lets the
 * implementation concentrate settings-write policy in one place.
 *
 * The actual DataStore I/O lives in [SettingsDataStore] and the mapping
 * between [AppSettings] and raw [androidx.datastore.preferences.core.Preferences]
 * lives in [SettingsMapper].
 */
class SettingsRepository(
    private val dataStore: SettingsDataStore,
    private val mapper: SettingsMapper = SettingsMapper()
) : HourlyScheduleSettingsStore {

    constructor(context: Context) : this(SettingsDataStore(context), SettingsMapper())

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        mapper.fromPreferences(prefs)
    }

    suspend fun runMigrations() {
        dataStore.editPreferences { prefs ->
            SettingsMigration.migrateInPlace(prefs)
        }
    }

    /**
     * Atomic update: reads current settings, applies [transform], writes back
     * all fields.
     *
     * Example: `update { it.copy(pitch = 0.5f, speechRate = 0.8f) }`
     */
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.editPreferences { prefs ->
            val current = mapper.fromPreferences(prefs)
            mapper.toPreferences(prefs, transform(current))
        }
    }
}
