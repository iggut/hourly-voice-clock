package com.hourlyvoiceclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
    )
)

/**
 * Thin Android-specific wrapper around the preferences [DataStore].
 *
 * This is the only module in the settings stack that knows about the
 * framework [Context]; everything above it works with [Preferences] or
 * [AppSettings].
 */
class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val data: Flow<Preferences> = dataStore.data

    suspend fun editPreferences(transform: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(transform)
    }
}
