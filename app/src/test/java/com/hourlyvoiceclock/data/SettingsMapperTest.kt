package com.hourlyvoiceclock.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsMapperTest {

    @Test
    fun `round-trip preserves all fields`() {
        val original = AppSettings(
            hourlyAnnouncementsEnabled = true,
            selectedVoiceName = "en-us-x-sfg-local",
            selectedLocale = "en-US",
            selectedVoicePresetId = "preset-1",
            selectedLocalModelId = "model-42",
            pitch = 1.2f,
            speechRate = 0.9f,
            timeFormat = TimeFormat.HOUR_24,
            phraseStyle = PhraseStyle.FRIENDLY,
            customPrefix = "Hey, it's ",
            customSuffix = " already",
            chimeSound = ChimeSound.BELL,
            vibrateBefore = true,
            announceDateOnDemand = true,
            quietHoursEnabled = true,
            quietHoursStart = LocalTime.of(23, 30),
            quietHoursEnd = LocalTime.of(6, 0),
            quietDaysQuietStart = LocalTime.of(9, 0),
            quietDaysQuietEnd = LocalTime.of(17, 0),
            allowManualDuringQuiet = false,
            quietDaysDisabled = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            exactAlarmsEnabled = true,
            notificationLogging = true,
            audioChannel = AudioChannel.CALL,
            selectedTtsEnginePackage = "com.google.android.tts",
            autoUpdateEnabled = false
        )

        val mapper = SettingsMapper()
        val prefs = mutablePreferencesOf()
        mapper.toPreferences(prefs, original)
        val restored = mapper.fromPreferences(prefs)

        assertEquals(original, restored)
    }

    @Test
    fun `blank optional strings are normalized to null`() {
        val mapper = SettingsMapper()
        val prefs = mutablePreferencesOf(
            SettingsMapper.KEY_SELECTED_VOICE_NAME to "   ",
            SettingsMapper.KEY_SELECTED_LOCALE to "",
            SettingsMapper.KEY_SELECTED_TTS_ENGINE_PACKAGE to "   "
        )

        val settings = mapper.fromPreferences(prefs)

        assertEquals(null, settings.selectedVoiceName)
        assertEquals(null, settings.selectedLocale)
        assertEquals(null, settings.selectedTtsEnginePackage)
    }

    @Test
    fun `default values are used for missing keys`() {
        val mapper = SettingsMapper()
        val settings = mapper.fromPreferences(mutablePreferencesOf())

        assertEquals(false, settings.hourlyAnnouncementsEnabled)
        assertEquals(TimeFormat.HOUR_12, settings.timeFormat)
        assertEquals(PhraseStyle.SIMPLE, settings.phraseStyle)
        assertEquals(AudioChannel.MEDIA, settings.audioChannel)
        assertEquals(ChimeSound.NONE, settings.chimeSound)
        assertEquals(LocalTime.of(22, 0), settings.quietHoursStart)
        assertEquals(LocalTime.of(7, 0), settings.quietHoursEnd)
        assertEquals(true, settings.allowManualDuringQuiet)
        assertEquals(true, settings.autoUpdateEnabled)
    }
}
