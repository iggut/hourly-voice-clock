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
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — for exact top-of-hour scheduling
- `RECEIVE_BOOT_COMPLETED` — to reschedule alarms after reboot
- `VIBRATE` — for pre-announcement vibration

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

No network calls are made by the app. The only network usage occurs if the user selects a TTS voice that itself requires network synthesis (indicated in the voice list).
