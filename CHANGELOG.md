# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-05-26

### Added
- **Exact top-of-hour alarms**: Full support for exact scheduling using Android's exact alarm API with permission checks.
- **Boot & Time Receiver Resilience**: Receivers for `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, and exact-alarm permission state changes keep the clock aligned.
- **Notification Logging**: Options to log announcement history to the notification tray. Includes complete runtime permission tracking (Android 13+).
- **Battery Optimization Card**: Reliability advice section in Settings explaining background execution restrictions, offering direct links to settings, and documenting setup steps for popular OEMs (Samsung, Xiaomi, OnePlus, Huawei, Vivo).
- **Auto-Update Checker**: Ability to poll GitHub releases for new update APK packages.
- **Material 3 UI Theme**: Modern dark/light glassmorphic layout detailing pitches, voice rates, phrase styles, and streams.
