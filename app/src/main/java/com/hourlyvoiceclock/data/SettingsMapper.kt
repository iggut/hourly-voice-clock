package com.hourlyvoiceclock.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Owns the bidirectional mapping between [AppSettings] and DataStore
 * [Preferences], including all preference keys and the pure helper
 * functions used during serialization.
 *
 * Keeping keys and mapping in one place removes duplication with
 * [SettingsMigration] and makes the mapping unit-testable with a plain
 * [Preferences] map, without needing Android or Robolectric.
 */
class SettingsMapper {

    fun fromPreferences(prefs: Preferences): AppSettings {
        return AppSettings(
            hourlyAnnouncementsEnabled = prefs[KEY_HOURLY_ANNOUNCEMENTS] ?: false,
            selectedVoiceName = prefs[KEY_SELECTED_VOICE_NAME]?.takeIf { it.isNotBlank() },
            selectedLocale = prefs[KEY_SELECTED_LOCALE]?.takeIf { it.isNotBlank() },
            selectedVoicePresetId = prefs[KEY_SELECTED_VOICE_PRESET_ID]?.takeIf { it.isNotBlank() },
            selectedLocalModelId = prefs[KEY_SELECTED_LOCAL_MODEL_ID]?.takeIf { it.isNotBlank() },
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
            quietDaysQuietStart = parseTime(prefs[KEY_QUIET_DAYS_START], "10:00"),
            quietDaysQuietEnd = parseTime(prefs[KEY_QUIET_DAYS_END], "18:00"),
            allowManualDuringQuiet = prefs[KEY_ALLOW_MANUAL_QUIET] ?: true,
            quietDaysDisabled = parseDayOfWeekSet(prefs[KEY_QUIET_DAYS_DISABLED]),
            exactAlarmsEnabled = prefs[KEY_EXACT_ALARMS] ?: false,
            notificationLogging = prefs[KEY_NOTIFICATION_LOGGING] ?: false,
            audioChannel = safeEnumValueOf(prefs[KEY_AUDIO_CHANNEL], AudioChannel.MEDIA),
            selectedTtsEnginePackage = prefs[KEY_SELECTED_TTS_ENGINE_PACKAGE]?.takeIf { it.isNotBlank() },
            autoUpdateEnabled = prefs[KEY_AUTO_UPDATE_ENABLED] ?: true,
            useDynamicColor = prefs[KEY_USE_DYNAMIC_COLOR] ?: false
        )
    }

    fun toPreferences(prefs: MutablePreferences, settings: AppSettings) {
        prefs[KEY_HOURLY_ANNOUNCEMENTS] = settings.hourlyAnnouncementsEnabled
        if (settings.selectedVoiceName != null) prefs[KEY_SELECTED_VOICE_NAME] = settings.selectedVoiceName else prefs.remove(KEY_SELECTED_VOICE_NAME)
        if (settings.selectedLocale != null) prefs[KEY_SELECTED_LOCALE] = settings.selectedLocale else prefs.remove(KEY_SELECTED_LOCALE)
        if (settings.selectedVoicePresetId != null) prefs[KEY_SELECTED_VOICE_PRESET_ID] = settings.selectedVoicePresetId else prefs.remove(KEY_SELECTED_VOICE_PRESET_ID)
        if (settings.selectedLocalModelId != null) prefs[KEY_SELECTED_LOCAL_MODEL_ID] = settings.selectedLocalModelId else prefs.remove(KEY_SELECTED_LOCAL_MODEL_ID)
        prefs[KEY_PITCH] = settings.pitch
        prefs[KEY_SPEECH_RATE] = settings.speechRate
        prefs[KEY_TIME_FORMAT] = settings.timeFormat.name
        prefs[KEY_PHRASE_STYLE] = settings.phraseStyle.name
        prefs[KEY_CUSTOM_PREFIX] = settings.customPrefix
        prefs[KEY_CUSTOM_SUFFIX] = settings.customSuffix
        prefs[KEY_CHIME_SOUND] = settings.chimeSound.name
        prefs[KEY_VIBRATE_BEFORE] = settings.vibrateBefore
        prefs[KEY_ANNOUNCE_DATE] = settings.announceDateOnDemand
        prefs[KEY_QUIET_HOURS_ENABLED] = settings.quietHoursEnabled
        prefs[KEY_QUIET_HOURS_START] = formatTime(settings.quietHoursStart)
        prefs[KEY_QUIET_HOURS_END] = formatTime(settings.quietHoursEnd)
        prefs[KEY_QUIET_DAYS_START] = formatTime(settings.quietDaysQuietStart)
        prefs[KEY_QUIET_DAYS_END] = formatTime(settings.quietDaysQuietEnd)
        prefs[KEY_ALLOW_MANUAL_QUIET] = settings.allowManualDuringQuiet
        prefs[KEY_QUIET_DAYS_DISABLED] = formatDayOfWeekSet(settings.quietDaysDisabled)
        prefs[KEY_EXACT_ALARMS] = settings.exactAlarmsEnabled
        prefs[KEY_NOTIFICATION_LOGGING] = settings.notificationLogging
        prefs[KEY_AUDIO_CHANNEL] = settings.audioChannel.name
        if (settings.selectedTtsEnginePackage != null) prefs[KEY_SELECTED_TTS_ENGINE_PACKAGE] = settings.selectedTtsEnginePackage else prefs.remove(KEY_SELECTED_TTS_ENGINE_PACKAGE)
        prefs[KEY_AUTO_UPDATE_ENABLED] = settings.autoUpdateEnabled
        prefs[KEY_USE_DYNAMIC_COLOR] = settings.useDynamicColor
    }

    companion object {
        internal val KEY_HOURLY_ANNOUNCEMENTS = booleanPreferencesKey("hourly_announcements")
        internal val KEY_SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
        internal val KEY_SELECTED_LOCALE = stringPreferencesKey("selected_locale")
        internal val KEY_SELECTED_VOICE_PRESET_ID = stringPreferencesKey("selected_voice_preset_id")
        internal val KEY_SELECTED_LOCAL_MODEL_ID = stringPreferencesKey("selected_local_model_id")
        internal val KEY_PITCH = floatPreferencesKey("pitch")
        internal val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        internal val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        internal val KEY_PHRASE_STYLE = stringPreferencesKey("phrase_style")
        internal val KEY_CUSTOM_PREFIX = stringPreferencesKey("custom_prefix")
        internal val KEY_CUSTOM_SUFFIX = stringPreferencesKey("custom_suffix")
        internal val KEY_CHIME_SOUND = stringPreferencesKey("chime_sound")
        internal val KEY_VIBRATE_BEFORE = booleanPreferencesKey("vibrate_before")
        internal val KEY_ANNOUNCE_DATE = booleanPreferencesKey("announce_date")
        internal val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        internal val KEY_QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        internal val KEY_QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
        internal val KEY_QUIET_DAYS_START = stringPreferencesKey("quiet_days_start")
        internal val KEY_QUIET_DAYS_END = stringPreferencesKey("quiet_days_end")
        internal val KEY_ALLOW_MANUAL_QUIET = booleanPreferencesKey("allow_manual_quiet")
        internal val KEY_QUIET_DAYS_DISABLED = stringPreferencesKey("quiet_days_disabled")
        internal val KEY_EXACT_ALARMS = booleanPreferencesKey("exact_alarms")
        internal val KEY_NOTIFICATION_LOGGING = booleanPreferencesKey("notification_logging")
        internal val KEY_AUDIO_CHANNEL = stringPreferencesKey("audio_channel")
        internal val KEY_SELECTED_TTS_ENGINE_PACKAGE = stringPreferencesKey("selected_tts_engine_package")
        internal val KEY_AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        internal val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")

        fun parseTime(value: String?, fallback: String): LocalTime {
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

        fun formatTime(time: LocalTime): String {
            val h = time.hour.toString().padStart(2, '0')
            val m = time.minute.toString().padStart(2, '0')
            return "$h:$m"
        }

        fun parseDayOfWeekSet(value: String?): Set<DayOfWeek> {
            if (value.isNullOrBlank()) return emptySet()
            return try {
                value.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }

        fun formatDayOfWeekSet(days: Set<DayOfWeek>): String {
            return days.joinToString(",") { it.name }
        }

        inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, fallback: T): T {
            if (value == null) return fallback
            return try {
                java.lang.Enum.valueOf(T::class.java, value)
            } catch (e: IllegalArgumentException) {
                fallback
            }
        }
    }
}
