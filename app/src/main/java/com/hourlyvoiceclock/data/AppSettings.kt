package com.hourlyvoiceclock.data

import java.time.LocalTime

enum class PhraseStyle {
    SIMPLE,
    DETAILED,
    FRIENDLY,
    CUSTOM
}

enum class TimeFormat {
    HOUR_12,
    HOUR_24
}

enum class AudioChannel {
    MEDIA,
    NOTIFICATION,
    CALL
}

enum class ChimeSound {
    NONE,
    CLASSIC_CHIME,
    BELL,
    GONG,
    CYMBALS,
    DIGITAL_BEEP,
    BIRD_CHIRP,
    HONK
}

data class AppSettings(
    val hourlyAnnouncementsEnabled: Boolean = false,
    val selectedVoiceName: String? = null,
    val selectedLocale: String? = null,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val timeFormat: TimeFormat = TimeFormat.HOUR_12,
    val phraseStyle: PhraseStyle = PhraseStyle.SIMPLE,
    val customPrefix: String = "It is now ",
    val customSuffix: String = "",
    val chimeSound: ChimeSound = ChimeSound.NONE,
    val vibrateBefore: Boolean = false,
    val announceDateOnDemand: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),
    val allowManualDuringQuiet: Boolean = true,
    val exactAlarmsEnabled: Boolean = false,
    val notificationLogging: Boolean = false,
    val audioChannel: AudioChannel = AudioChannel.MEDIA,
    val selectedTtsEnginePackage: String? = null,
    val autoUpdateEnabled: Boolean = true
)
