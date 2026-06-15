# Changelog

All notable changes to this project will be documented in this file.

## [0.4.5-alpha] - 2026-06-15

### Fixed
- **Local-voice download was silently failing** — `VoiceModelRegistry` pointed at `https://github.com/rhasspy/piper/releases/download/v2.0.0/*.onnx`, which returns HTTP 404 (Piper's GitHub releases only host CLI tarballs, not model weights, and there is no `v2.0.0` tag). The click did nothing visible because:
  1. The download URL was wrong.
  2. The downloader only fetched the `.onnx` file but the model needs its sibling `.onnx.json` (audio config + phoneme id map) to load.
  3. The error was only written to logcat — the screen had no error state to display, so the button just reset to "Download" with no feedback.
- **`OnnxModelDownloader` rewritten to fetch both files** (`.onnx` + `.onnx.json`) into the model directory, with existence-based early-out (not size-based, so a partial download will be re-fetched), proper IOException → `DownloadException` translation, and cancellation support via `currentCoroutineContext().ensureActive()`.
- **Registry URLs moved to the HuggingFace `rhasspy/piper-voices` mirror** for all 6 voices. Sizes updated to the actual HF content-lengths (63–78 MB).
- **Download errors now surface inline** — `LocalVoiceSettingsViewModel` exposes `errorsByModelId: StateFlow<Map<String, String>>`; the card renders an `ErrorChip` with the failure reason and the button switches to "Retry download" until the user dismisses.
- **A "Preview build" banner** is shown on the Local Voices screen explaining that the wiring into the hourly announcement flow lands in 0.5.x.

### Changed
- `VoiceModel` gains `onnxFileName`, `onnxJsonFileName`, `onnxDownloadUrl`, `onnxJsonDownloadUrl` (was a single `downloadUrl` + `fileName` pair).
- `LocalTtsEngine` updated to read from the new field names.

### Added
- `DownloadException` (typed exception with optional cause) for downloader errors.
- `OnnxModelDownloaderTest` with 10 Robolectric tests covering URL/file-name consistency, the existence check across the four file states, deletion, and the `DownloadException` shape.

### Verified
- `./gradlew test` — BUILD SUCCESSFUL, 10 new tests pass.
- `./gradlew compileDebugKotlin` — clean.
- Live URL check: `https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx` returns HTTP 200 / 63,201,294 bytes.

## [0.4.4-alpha] - 2026-06-13

### Added
- **Quiet-hours day-of-week overrides**: `AppSettings` gains a `quietDaysDisabled: Set<DayOfWeek>` so the user can mute announcements on specific days (e.g. weekends) while leaving manual announcements available. Backed by a new `KEY_QUIET_DAYS_DISABLED` DataStore preference and a quiet-day picker in `ScheduleSettingsScreen`. `QuietHoursPolicy` now takes the current day and the disabled-day set; tests cover both the scheduled- and manual-announcement branches.
- **Local TTS scaffold (Sherpa-ONNX, Phase 1)**: New `tts.local` package (`LocalTtsEngine`, `OnnxModelDownloader`, `VoiceModelRegistry`) and `ui.localvoices` package (`LocalVoiceSettingsScreen` + ViewModel) implement the on-device voice layer described in `docs/local-voice-llm-plan.md`. The engine wraps `sherpa-onnx-1.13.2.aar` (added as a file dependency under `app/libs/`, gitignored). A "Local voices" route is reachable from `VoiceSettingsScreen`. The engine is **not yet wired into `TtsEngineSelector`**; this release exposes the feature surface so the rest of the app can adopt it in 0.5.x.
- **Voice settings → local voices entry** in `VoiceSettingsScreen`.

### Changed
- **`TimeAnnouncer` passes the current `dayOfWeek` and the disabled-day set** into `QuietHoursPolicy` so scheduled announcements honor per-day overrides.
- **`SettingsRepository`** reads/writes the new preference using a comma-joined enum codec.
- **Android CI no longer runs on tag push.** The workflow is now `workflow_dispatch` only; CI is invoked manually with a `version_tag` input. Tag pushes do not start a build, and the release APK is published by the local release process instead. Avoids accidental double-builds and ensures CI only runs when intentionally triggered.

## [0.4.3-alpha] - 2026-06-10

### Added
- **Android App Bundle (AAB) support** for Play Store submission. `bundleRelease` produces a signed .aab; the legacy `assembleRelease` APK is still produced for sideloading.
- **R8 minification + resource shrinking** for release builds. APK drops from ~11 MB to ~1.2 MB; AAB is 3.2 MB.
- **Lint baseline** so the 103 pre-existing lint warnings do not block release; new warnings will fail the build.
- **Privacy policy URL string** (`R.string.privacy_policy_url`) for Play Console. Replace the placeholder before submitting.
- **`versionCode` / `versionName` overridable from `gradle.properties`** so CI can stamp builds from the version tag without editing `build.gradle.kts`.
- **Language resource filter** (`en`, `fr`) for an accurate Play Store "supported languages" listing.

### Changed
- **Compose `Modifier.padding` lint baseline**: documented pre-existing issues rather than disabling the lint check.

### Notes for Play Console submission
- Replace `R.string.privacy_policy_url` with the published URL of your privacy policy.
- Data safety: declare "no data collected" — the app makes no analytics or telemetry calls. Two network endpoints are used, both opt-in: GitHub releases API (auto-update check) and network-required TTS voices.
- Content rating: IARC questionnaire in the Play Console — answer "no" to all data collection questions.
- Signing: set `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in CI secrets. The local debug-keystore fallback is for development only.

## [0.3.0] - 2026-05-29

### Added
- **eSpeak NG Voice Variants**: When eSpeak NG engine is selected, 12 distinct fun voice variants become available: Robot, Alien, Monster, Cartoon, Deep Voice, Chipmunk, Ghost, Dwarf, Evil, Wizard, Baby, Robot Female
- **eSpeak NG Engine Priority**: eSpeak NG is now the primary alternative TTS engine, optimized for fun voice effects

### Removed
- **RHVoice Engine**: Removed from known TTS engines list (functionality replaced by eSpeak NG variants)

### Fixed
- **Seconds indicator animation**: Changed from 0.2f→1.0f at 1000ms to 0.6f→1.0f at 2000ms for better visibility; static solid 1.0f when hourly announcements are active
- **Main clock visual hierarchy**: Moved "Announce Now" button inside clock card as footer element
- **Redundant "Quick Settings" label**: Removed section title, tightened padding from 20dp to 12dp
- **Material Icons consistency**: All icons now use consistent Icons.Filled style
- **Dark mode text contrast**: onSurface adjusted to Color(0xFFF1F5F9).copy(alpha = 0.7f) for better contrast with onBackground
- **Glassmorphism light mode**: GlassBgLight alpha reduced from 0.7f to 0.5f for legible glass effect
- **Icon box dark mode contrast**: Dark mode icon boxes now use onSurface.copy(alpha = 0.1f) for better visibility

### Changed
- **Brand typography**: Added fontFeatureSettings = "tnum" for tabular figures on seconds display

## [0.1.0] - 2026-05-26

### Added
- **Exact top-of-hour alarms**: Full support for exact scheduling using Android's exact alarm API with permission checks.
- **Boot & Time Receiver Resilience**: Receivers for `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, and exact-alarm permission state changes keep the clock aligned.
- **Notification Logging**: Options to log announcement history to the notification tray. Includes complete runtime permission tracking (Android 13+).
- **Battery Optimization Card**: Reliability advice section in Settings explaining background execution restrictions, offering direct links to settings, and documenting setup steps for popular OEMs (Samsung, Xiaomi, OnePlus, Huawei, Vivo).
- **Auto-Update Checker**: Ability to poll GitHub releases for new update APK packages.
- **Material 3 UI Theme**: Modern dark/light glassmorphic layout detailing pitches, voice rates, phrase styles, and streams.
