# Hourly Voice Clock

A native Android app that announces the current time using selectable system Text-to-Speech voices. Supports hourly scheduling, quiet hours, multiple phrase styles, and Material 3 UI.

## Features

- **Big time display** on the home screen
- **Manual announcement** with "Announce time now"
- **Hourly announcements** at the top of the hour via AlarmManager
- **Voice selection** grouped by locale/accent with quality/latency info
- **Pitch and speech rate** sliders with live preview
- **Phrase styles**: Simple, Detailed, Friendly
- **12-hour and 24-hour** time formats
- **Optional chime and vibrate** before speaking
- **Quiet hours** with midnight-crossing support
- **Exact alarm support** with graceful fallback to inexact alarms
- **Resilient scheduling** across reboots, timezone changes, and app updates

## Architecture

```
com.hourlyvoiceclock/
  data/           # DataStore settings repository
  tts/            # TextToSpeech wrapper and voice repository
  scheduler/      # AlarmManager scheduling
  receiver/       # Boot, Alarm, and TimeChanged receivers
  announcer/      # Business logic: formatter, quiet hours, time announcer
  ui/             # Jetpack Compose screens and ViewModels
```

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- DataStore (preferences)
- AlarmManager
- Android TextToSpeech
- JUnit + Espresso

## Setup

1. Open the project in Android Studio (Giraffe or newer).
2. Sync Gradle.
3. Run on an emulator or device with API 26+.

## Permissions

- `POST_NOTIFICATIONS` (Android 13+) — only if notification logging is enabled
- `SCHEDULE_EXACT_ALARM` — for exact top-of-hour scheduling
- `RECEIVE_BOOT_COMPLETED` — to reschedule alarms after reboot
- `VIBRATE` — for pre-announcement vibration
- `INTERNET` — to check the public GitHub releases API for app updates

## Android Version Notes

- **API 26+ (Android 8.0)** required
- **API 31+ (Android 12)** exact alarms require user grant via "Alarms & reminders" permission
- The app gracefully falls back to inexact alarms if exact permission is unavailable
- No permanent foreground service is used

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Voice Selection

The app queries real voices from the installed Android TTS engine using `TextToSpeech.getVoices()`. If only one voice is available, an empty-state message guides the user to Android TTS settings to install more voices.

Gender metadata is not guaranteed by the Android TTS API. The app displays "Male" or "Female" only when clearly present in the voice name; otherwise voices are labeled "Voice 1", "Voice 2", etc.

## Privacy

No personal data or usage metrics are collected. The only network requests made by the app are to query the latest public GitHub release for app updates (if auto-check is enabled in settings) and if a TTS voice itself requires network synthesis (indicated in the voice list).

## Release QA Matrix

To verify build reliability, manual checks must be performed across these target segments prior to tagging a new release:

| Test Scenario | Pixel (Android 14) | Samsung (One UI) | Xiaomi (MIUI) | Expected Outcome |
| :--- | :--- | :--- | :--- | :--- |
| **Exact Alarm Toggling** | Yes | Yes | Yes | Saved immediately to DataStore; triggers system settings prompt if permission missing. |
| **Notification Logging** | Yes | Yes | Yes | Asks `POST_NOTIFICATIONS` runtime permission; writes log entry to drawer; silently skipped if denied. |
| **Battery Optimization** | Yes | Yes | Yes | Detects optimization state; links to system battery optimizations screen. |
| **OEM Background Launch** | N/A | Exclude from App Sleeping | Enable Autostart | Alarms scheduled via `AlarmManager` execute reliably on time. |
| **Boot Recovery** | Yes | Yes | Yes | Re-arms next scheduled top-of-hour announcement automatically after device restart. |

### Release Automation

GitHub release builds are automated using a GitHub Actions CI pipeline:
- **Triggers**: Creating and pushing a version tag (e.g. `v0.1`) runs test verification, compiles release builds, and outputs release packages.
- **Artifact Format**: Outputs `hourly-voice-clock-v[version]-release.apk` along with its `hourly-voice-clock-v[version]-release.apk.sha256` verification checksum file.
- **Publishing**: Automatically creates a draft release containing these build artifacts ready for approval.
