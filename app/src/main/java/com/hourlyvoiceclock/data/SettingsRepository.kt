package com.hourlyvoiceclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
    )
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            hourlyAnnouncementsEnabled = prefs[KEY_HOURLY_ANNOUNCEMENTS] ?: false,
            selectedVoiceName = prefs[KEY_SELECTED_VOICE_NAME]?.takeIf { it.isNotBlank() },
            selectedLocale = prefs[KEY_SELECTED_LOCALE]?.takeIf { it.isNotBlank() },
            selectedVoicePresetId = prefs[KEY_SELECTED_VOICE_PRESET_ID]?.takeIf { it.isNotBlank() },
            pitch = prefs[KEY_PITCH] ?: 1.0f,
            speechRate = prefs[KEY_SPEECH_RATE] ?: 1.0f,
            timeFormat = safeEnumValueOf(prefs[KEY_TIME_FORMAT], TimeFormat.HOUR_12),
            phraseStyle = safeEnumValueOf(prefs[KEY_PHRASE_STYLE], PhraseStyle.SIMPLE),
            customPrefix = prefs[KEY_CUSTOM_PREFIX] ?: "It is now ",
            customSuffix = prefs[KEY_CUSTOM_SUFFIX] ?: "",
            chimeSound = safeEnumValueOf(prefs[KEY_CHIME_SOUND], ChimeSound.NONE),
            vibrateBefore = prefs[KEY_VIBRATE_BEFORE] ?: false,
            announceDateOnDemand = prefs[KEY_ANNOUNCE_DATE] ?: false,
            quietHoursEnabled = prefs[KEY_QUIET_HOURS_ENABLED] ?: false,
            quietHoursStart = parseTime(prefs[KEY_QUIET_HOURS_START], "22:00"),
            quietHoursEnd = parseTime(prefs[KEY_QUIET_HOURS_END], "07:00"),
            allowManualDuringQuiet = prefs[KEY_ALLOW_MANUAL_QUIET] ?: true,
            exactAlarmsEnabled = prefs[KEY_EXACT_ALARMS] ?: false,
            notificationLogging = prefs[KEY_NOTIFICATION_LOGGING] ?: false,
            audioChannel = safeEnumValueOf(prefs[KEY_AUDIO_CHANNEL], AudioChannel.MEDIA),
            selectedTtsEnginePackage = prefs[KEY_SELECTED_TTS_ENGINE_PACKAGE]?.takeIf { it.isNotBlank() },
            autoUpdateEnabled = prefs[KEY_AUTO_UPDATE_ENABLED] ?: true
        )
    }

    suspend fun setHourlyAnnouncements(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HOURLY_ANNOUNCEMENTS] = enabled }
    }

    suspend fun runMigrations() {
        context.dataStore.edit { prefs ->
            SettingsMigration.migrateInPlace(prefs)
        }
    }

    suspend fun setSelectedVoice(voiceName: String?, locale: String?) {
        context.dataStore.edit {
            it[KEY_SELECTED_VOICE_NAME] = voiceName ?: ""
            it[KEY_SELECTED_LOCALE] = locale ?: ""
            it.remove(KEY_SELECTED_VOICE_PRESET_ID)
        }
    }

    suspend fun setSelectedVoicePreset(presetId: String?) {
        context.dataStore.edit {
            if (presetId.isNullOrBlank()) {
                it.remove(KEY_SELECTED_VOICE_PRESET_ID)
            } else {
                it[KEY_SELECTED_VOICE_PRESET_ID] = presetId
            }
        }
    }

    suspend fun setPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_PITCH] = pitch }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        context.dataStore.edit { it[KEY_TIME_FORMAT] = format.name }
    }

    suspend fun setPhraseStyle(style: PhraseStyle) {
        context.dataStore.edit { it[KEY_PHRASE_STYLE] = style.name }
    }

    suspend fun setCustomPrefix(prefix: String) {
        context.dataStore.edit { it[KEY_CUSTOM_PREFIX] = prefix }
    }

    suspend fun setCustomSuffix(suffix: String) {
        context.dataStore.edit { it[KEY_CUSTOM_SUFFIX] = suffix }
    }

    suspend fun setChimeSound(sound: ChimeSound) {
        context.dataStore.edit { it[KEY_CHIME_SOUND] = sound.name }
    }

    suspend fun setVibrateBefore(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATE_BEFORE] = enabled }
    }

    suspend fun setAnnounceDateOnDemand(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ANNOUNCE_DATE] = enabled }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_QUIET_HOURS_ENABLED] = enabled }
    }

    suspend fun setQuietHoursStart(time: LocalTime) {
        context.dataStore.edit { it[KEY_QUIET_HOURS_START] = formatTime(time) }
    }

    suspend fun setQuietHoursEnd(time: LocalTime) {
        context.dataStore.edit { it[KEY_QUIET_HOURS_END] = formatTime(time) }
    }

    suspend fun setAllowManualDuringQuiet(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALLOW_MANUAL_QUIET] = enabled }
    }

    suspend fun setExactAlarmsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EXACT_ALARMS] = enabled }
    }

    suspend fun setNotificationLogging(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATION_LOGGING] = enabled }
    }

    suspend fun setAudioChannel(channel: AudioChannel) {
        context.dataStore.edit { it[KEY_AUDIO_CHANNEL] = channel.name }
    }

    suspend fun setSelectedTtsEnginePackage(packageName: String?) {
        context.dataStore.edit { it[KEY_SELECTED_TTS_ENGINE_PACKAGE] = packageName ?: "" }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_UPDATE_ENABLED] = enabled }
    }


    companion object {
        internal fun parseTime(value: String?, fallback: String): LocalTime {
            val cleanValue = if (value.isNullOrBlank()) fallback else value
            return try {
                val parts = cleanValue.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) {
                try {
                    val parts = fallback.split(":")
                    LocalTime.of(parts[0].toInt(), parts[1].toInt())
                } catch (e2: Exception) {
                    LocalTime.of(22, 0)
                }
            }
        }

        internal fun formatTime(time: LocalTime): String {
            return String.format("%02d:%02d", time.hour, time.minute)
        }

        internal inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, fallback: T): T {
            if (value == null) return fallback
            return try {
                java.lang.Enum.valueOf(T::class.java, value)
            } catch (e: IllegalArgumentException) {
                fallback
            }
        }
        private val KEY_HOURLY_ANNOUNCEMENTS = booleanPreferencesKey("hourly_announcements")
        private val KEY_SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
        private val KEY_SELECTED_LOCALE = stringPreferencesKey("selected_locale")
        private val KEY_SELECTED_VOICE_PRESET_ID = stringPreferencesKey("selected_voice_preset_id")
        private val KEY_PITCH = floatPreferencesKey("pitch")
        private val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        private val KEY_PHRASE_STYLE = stringPreferencesKey("phrase_style")
        private val KEY_CUSTOM_PREFIX = stringPreferencesKey("custom_prefix")
        private val KEY_CUSTOM_SUFFIX = stringPreferencesKey("custom_suffix")
        private val KEY_CHIME_SOUND = stringPreferencesKey("chime_sound")
        private val KEY_VIBRATE_BEFORE = booleanPreferencesKey("vibrate_before")
        private val KEY_ANNOUNCE_DATE = booleanPreferencesKey("announce_date")
        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        private val KEY_QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
        private val KEY_ALLOW_MANUAL_QUIET = booleanPreferencesKey("allow_manual_quiet")
        private val KEY_EXACT_ALARMS = booleanPreferencesKey("exact_alarms")
        private val KEY_NOTIFICATION_LOGGING = booleanPreferencesKey("notification_logging")
        private val KEY_AUDIO_CHANNEL = stringPreferencesKey("audio_channel")
        private val KEY_SELECTED_TTS_ENGINE_PACKAGE = stringPreferencesKey("selected_tts_engine_package")
        private val KEY_AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
    }
}
