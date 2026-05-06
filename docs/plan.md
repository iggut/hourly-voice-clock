# Hourly Voice Clock — Implementation Plan

> **Goal:** Build a native Android app that announces the current time using a selectable system TTS voice, with hourly scheduling, quiet hours, and Material 3 UI.

**Architecture:** Clean architecture with `ui/`, `data/`, `tts/`, `scheduler/`, `receiver/`, `announcer/` packages. DataStore for settings, AlarmManager for scheduling, TextToSpeech for voice output.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, DataStore, Room (not strictly needed but included), AlarmManager, TextToSpeech, JUnit, Espresso.

---

## File Structure

```
app/src/main/java/com/hourlyvoiceclock/
  MainActivity.kt
  HourlyVoiceClockApp.kt
  data/
    AppSettings.kt
    SettingsRepository.kt
    SettingsSerializer.kt
  tts/
    TtsEngine.kt
    TtsVoiceRepository.kt
    VoiceInfo.kt
  scheduler/
    AnnouncementScheduler.kt
    AlarmPermissionChecker.kt
  receiver/
    AlarmReceiver.kt
    BootReceiver.kt
    TimeChangedReceiver.kt
  announcer/
    TimeAnnouncer.kt
    AnnouncementFormatter.kt
    QuietHoursPolicy.kt
  ui/
    theme/
    home/
    voicesettings/
    formatsettings/
    schedulesettings/
    components/
    navigation/
app/src/test/java/com/hourlyvoiceclock/
  QuietHoursPolicyTest.kt
  AnnouncementFormatterTest.kt
  NextHourCalculationTest.kt
  FakeTtsEngine.kt
app/src/androidTest/java/com/hourlyvoiceclock/
  HomeScreenTest.kt
```

## Task Batches

### Batch 1: Project Scaffold
- Root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- App `build.gradle.kts`
- `AndroidManifest.xml` with permissions and receivers
- Gradle wrapper properties

### Batch 2: Data Layer
- `AppSettings` data class
- `SettingsSerializer` for DataStore
- `SettingsRepository` with Flow-based API

### Batch 3: TTS Layer
- `VoiceInfo` model
- `TtsEngine` interface + Android implementation
- `TtsVoiceRepository` for querying/setting voices
- `FakeTtsEngine` for tests

### Batch 4: Business Logic
- `QuietHoursPolicy` with midnight-crossing logic
- `AnnouncementFormatter` (12h/24h, simple/detailed/friendly)
- `TimeAnnouncer` orchestrating TTS + formatter

### Batch 5: Scheduler + Receivers
- `AlarmPermissionChecker`
- `AnnouncementScheduler` using AlarmManager
- `AlarmReceiver`, `BootReceiver`, `TimeChangedReceiver`

### Batch 6: UI Layer
- Theme files
- Navigation setup
- Home screen + ViewModel
- Voice settings screen + ViewModel
- Format settings screen + ViewModel
- Schedule settings screen + ViewModel
- Shared components

### Batch 7: Tests
- Unit tests for policy, formatter, scheduler math
- Instrumented test for manual announcement
- Fake TTS verification

### Batch 8: README + Final Polish
- README with setup, permissions, Android version notes
- String resources finalized
- Final manifest verification
