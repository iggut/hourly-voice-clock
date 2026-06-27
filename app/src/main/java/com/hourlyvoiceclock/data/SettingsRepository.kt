package com.hourlyvoiceclock.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Public settings seam. Exposes the user's [AppSettings] as a [Flow] and
 * provides convenient write methods for individual fields, plus an atomic
 * [update] transform for multi-field changes.
 *
 * The actual DataStore I/O lives in [SettingsDataStore] and the mapping
 * between [AppSettings] and raw [androidx.datastore.preferences.core.Preferences]
 * lives in [SettingsMapper]. This class is a thin policy coordinator that
 * delegates to both.
 */
class SettingsRepository(
    private val dataStore: SettingsDataStore,
    private val mapper: SettingsMapper = SettingsMapper()
) : HourlyScheduleSettingsStore {

    constructor(context: Context) : this(SettingsDataStore(context), SettingsMapper())

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        mapper.fromPreferences(prefs)
    }

    override suspend fun setHourlyAnnouncements(enabled: Boolean) {
        update { it.copy(hourlyAnnouncementsEnabled = enabled) }
    }

    suspend fun runMigrations() {
        dataStore.editPreferences { prefs ->
            SettingsMigration.migrateInPlace(prefs)
        }
    }

    suspend fun setSelectedVoice(voiceName: String?, locale: String?) {
        update {
            it.copy(
                selectedVoiceName = voiceName,
                selectedLocale = locale,
                selectedVoicePresetId = null
            )
        }
    }

    suspend fun setSelectedVoicePreset(presetId: String?) {
        update { it.copy(selectedVoicePresetId = presetId?.takeIf { it.isNotBlank() }) }
    }

    /**
     * Persist the user's choice of a downloaded on-device voice. A
     * `null` value means "use the system TTS path" (the default).
     */
    suspend fun setSelectedLocalModelId(modelId: String?) {
        update { it.copy(selectedLocalModelId = modelId?.takeIf { it.isNotBlank() }) }
    }

    suspend fun setPitch(pitch: Float) {
        update { it.copy(pitch = pitch) }
    }

    suspend fun setSpeechRate(rate: Float) {
        update { it.copy(speechRate = rate) }
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        update { it.copy(timeFormat = format) }
    }

    suspend fun setPhraseStyle(style: PhraseStyle) {
        update { it.copy(phraseStyle = style) }
    }

    suspend fun setCustomPrefix(prefix: String) {
        update { it.copy(customPrefix = prefix) }
    }

    suspend fun setCustomSuffix(suffix: String) {
        update { it.copy(customSuffix = suffix) }
    }

    suspend fun setChimeSound(sound: ChimeSound) {
        update { it.copy(chimeSound = sound) }
    }

    suspend fun setVibrateBefore(enabled: Boolean) {
        update { it.copy(vibrateBefore = enabled) }
    }

    suspend fun setAnnounceDateOnDemand(enabled: Boolean) {
        update { it.copy(announceDateOnDemand = enabled) }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        update { it.copy(quietHoursEnabled = enabled) }
    }

    suspend fun setQuietHoursStart(time: LocalTime) {
        update { it.copy(quietHoursStart = time) }
    }

    suspend fun setQuietHoursEnd(time: LocalTime) {
        update { it.copy(quietHoursEnd = time) }
    }

    suspend fun setQuietDaysQuietStart(time: LocalTime) {
        update { it.copy(quietDaysQuietStart = time) }
    }

    suspend fun setQuietDaysQuietEnd(time: LocalTime) {
        update { it.copy(quietDaysQuietEnd = time) }
    }

    suspend fun setAllowManualDuringQuiet(enabled: Boolean) {
        update { it.copy(allowManualDuringQuiet = enabled) }
    }

    suspend fun setQuietDaysDisabled(days: Set<DayOfWeek>) {
        update { it.copy(quietDaysDisabled = days) }
    }

    override suspend fun setExactAlarmsEnabled(enabled: Boolean) {
        update { it.copy(exactAlarmsEnabled = enabled) }
    }

    suspend fun setNotificationLogging(enabled: Boolean) {
        update { it.copy(notificationLogging = enabled) }
    }

    suspend fun setAudioChannel(channel: AudioChannel) {
        update { it.copy(audioChannel = channel) }
    }

    suspend fun setSelectedTtsEnginePackage(packageName: String?) {
        update { it.copy(selectedTtsEnginePackage = packageName) }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        update { it.copy(autoUpdateEnabled = enabled) }
    }

    /**
     * Atomic update: reads current settings, applies [transform], writes back
     * all fields. More concise than calling individual setters when multiple
     * fields change together.
     *
     * Example: `update { it.copy(pitch = 0.5f, speechRate = 0.8f) }`
     */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.editPreferences { prefs ->
            val current = mapper.fromPreferences(prefs)
            mapper.toPreferences(prefs, transform(current))
        }
    }
}
