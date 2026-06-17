# Changelog

All notable changes to this project will be documented in this file.

## [0.4.31-alpha] - 2026-06-17

### Tests
- **Align `OnnxModelDownloaderTest` with the v4 loader marker.** `isModelDownloaded` now requires a `.local-tts-loader-v4` marker; tests that simulate a "fully downloaded" voice write it. The 16-byte ONNX stubs the old tests used are rejected by `validateModelFiles()` (size below the `max(1MB, sizeBytes/5)` threshold), so the helper now writes a 16MB stub plus a real Piper `.onnx.json` with a non-empty `phoneme_id_map` to pass the same validation a real download would.
- **New test: `isModelDownloaded returns false when current loader marker is missing`.** Mirrors the force-redownload contract in `OnnxModelDownloader.isModelDownloaded` — a model directory with files but no marker is treated as not downloaded.

## [0.4.30-alpha] - 2026-06-17

### Fixed
- **Crash when pressing play/preview on downloaded local voices.** The previous metadata migration tried to strip legacy ONNX metadata by parsing and rewriting the ONNX protobuf by hand. That parser only handled one-byte field tags, while real ONNX `ModelProto` files contain multi-byte tags, so it could corrupt downloaded model files before Sherpa-ONNX loaded them. The fix removes the unsafe metadata rewrite completely and only appends missing metadata keys.
- **Force clean re-download after the unsafe patch.** Local voice downloads now require a current loader marker (`.local-tts-loader-v4`). Existing files created or touched by older local-TTS builds are treated as not installed, so the next Download press deletes and re-fetches clean upstream `.onnx`, `.onnx.json`, and `tokens.txt` files.

## [0.4.29-alpha] - 2026-06-17

### Fixed
- **Local voice preview no longer gets stuck on stale/corrupt “Installed” downloads.** The downloader now validates existing and freshly downloaded Piper files before treating them as installed. It rejects likely partial downloads, Git-LFS pointer files, HTML/API error payloads, and malformed `.onnx.json` files, then cleans stale files before retrying. This prevents cards such as JARVIS high from remaining in an installed-but-not-loadable state and forces a clean re-download.

## [0.4.28-alpha] - 2026-06-17

### Fixed
- **Crash on local voice preview/play — wrong ONNX metadata key.** The native Sherpa-ONNX code in `piper-phonemize/phonemize.cpp` calls `espeak_SetVoiceByName(meta_data.voice.c_str())`. The value of `meta_data.voice` is read from the **ONNX metadata key `voice`** (`SHERPA_ONNX_READ_META_DATA_STR_WITH_DEFAULT(meta_data_.voice, "voice", "")` in `offline-tts-vits-model.cc`). The previous patch (0.4.25-alpha, still in 0.4.27-alpha) set ONNX key `language` instead — that field is required but is not used for the eSpeak voice lookup. With `voice=""`, `espeak_SetVoiceByName("")` returns non-zero and the C++ runtime throws `std::runtime_error("Failed to set eSpeak-ng voice")`. The exception escapes the JNI boundary as a SIGABRT.
  - **Correct field:** the ONNX metadata key is now `voice` (not `language`). The value is read from `json.espeak.voice` (e.g. `en-us`), lowercased.
  - **Migration for already-downloaded models:** the new patch first strips the legacy `language` and `comment` entries that the broken 0.4.27-alpha patch added, then appends the correct `voice` entry. The other fields (`sample_rate`, `n_speakers`) are preserved.
  - **Fresh downloads:** the patch now writes the four correct entries on first use, so future installs need no migration.
  - **Defensive defaults:** if the JSON omits `espeak.voice`, the patch falls back to `en-us` (the rhasspy `piper-voices` default) rather than letting the native code call `espeak_SetVoiceByName("")`.

### Added
- `OnnxMetadataPatchTest` with 5 unit tests covering the protobuf structure (`metadata_props` field-14 tag, `StringStringEntryProto` field-1-then-2 layout), the post-patch key set (`voice`, `sample_rate`, `n_speakers`, `comment`), the absence of the legacy `language` key, and the regex that extracts `espeak.voice` from the JSON.

## [0.4.27-alpha] - 2026-06-17

### Fixed
- **Crash when previewing or playing downloaded local voices.** Piper `.onnx.json` files specify language codes with underscores (e.g. `en_US`). Sherpa-ONNX passes this raw string to eSpeak-ng's `espeak_SetVoiceByName`, which expects hyphens (`en-US`). The mismatch caused an uncaught C++ `std::runtime_error` that aborted the process.
  - New downloads now normalize `_` → `-` when patching ONNX metadata.
  - Existing downloaded models are auto-migrated on first use: any `en_US` in the ONNX file is rewritten to `en-US` in-place.
  - Also added default eSpeak voice files (`en-us`, `en`, `en-GB`, `en-gb`) to the bundled espeak-ng-data to ensure the voice lookup succeeds even on fresh installs.

## [0.4.26-alpha] - 2026-06-16

### Added
- **Curated community voice pack in Local Voices.** 17 additional Piper ONNX voices from the user's `~/Downloads/piper_community_voice_pack/manifest.json` are now offered alongside the existing rhasspy/piper-voices baseline. Mix of community and official sources:
  - **Assistant / AI**: JARVIS high, JARVIS medium mk1 (both MIT-licensed, en-GB).
  - **Sci-fi / game AI**: HAL 9000 (campwill, Apache-2.0), HAL 9000 denoised, GLaDOS high, Commander Data style, Captain Picard style, K9, BMO, BT-7274, Overwatch dispatch style, Subnautica PDA style, Kronk style, Rocket Raccoon style.
  - **Official Piper baselines (MIT)**: Ryan high, LJSpeech high, LibriTTS high.
  Each entry's description includes the source repository and a one-line rights note. Descriptions for fictional-character voices call out personal-testing-only usage so the user can make an informed choice.

## [0.4.25-alpha] - 2026-06-16

### Fixed
- **Hourly announcements no longer cut off at the start.** The first syllable of an announcement was being clipped because the TTS engine was dispatched the instant audio focus was granted — before the audio HAL had finished rerouting to the requested stream. `TimeAnnouncer` now waits 120 ms after focus is acquired before calling `speakAsync`, giving the routing layer time to land on the speech stream.
- **Friendly style greeting no longer collapses to "Hello" for late-night hours.** The `FRIENDLY` phrase style used to say "Hello" between 22:00 and 04:59, and only used a named greeting between 05:00 and 21:59. The bands are now widened so every hour maps to a named greeting: 05–11 morning, 12–16 afternoon, 17–21 evening, 22–04 night. A user at 03:00 now hears "Good night. It is 3 AM." instead of "Hello. It is 3 AM."
- **Downloaded on-device voices now appear as selectable voice options.** Previously, downloading a Piper voice in Local Voices only let you preview it — picking one for the hourly announcement required going through the system TTS path, which could not load Piper models. Downloaded voices are now listed in the main Voice Settings screen as a "Local AI Voices" section with an "On-device" badge; selecting one routes the hourly announcement through `LocalTtsEngine` with the chosen model. The Home screen subtitle updates to "Local: <Voice Name>" when a local voice is active. A "Use system voice" row is offered in the same section to fall back to the system TTS path.
