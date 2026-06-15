# Local Voice LLM for On-Device TTS

## Problem

The app currently relies on Android's built-in `TextToSpeech` API, which offers limited voice variety and no "fun" character voices. Users want expressive, entertaining local voices that work fully offline.

## Recommended Solution: Sherpa-ONNX + Piper/VITS Models

### Why Sherpa-ONNX

- **13k stars**, actively maintained (v1.13.2), Apache-2.0 licensed
- First-class **Android support** with Kotlin API
- Supports **ONNX TTS models** (Piper, VITS, Matcha-TTS, etc.)
- Runs entirely offline, no internet needed after model download
- Supports arm64-v8a (all modern phones) and armeabi-v7a
- Pre-built AAR available via JitPack/Maven
- Has **Piper voice models** (robot, alien, whisper, character voices) and VITS models

### Why Piper/VITS Models for "Fun Voices"

Piper models are ~15-50MB each, fast inference on ARM, and come in many character voices:
- `en_US-amy-medium` - Friendly female
- `en_US-lessac-medium` - Expressive male
- `en_US-libritts_r-medium` - Narrative style
- `en_GB-alba-medium` - British accent
- VITS models: `vits-piper-*` variants with character voices

## Architecture

```
┌─────────────────────────────────────────────┐
│              TtsEngine (existing)            │
│  interface: speak(), getVoices(), etc.       │
├─────────────────────┬───────────────────────┤
│  AndroidTtsEngine   │  LocalTtsEngine        │
│  (existing)         │  (NEW - wraps ONNX)   │
├─────────────────────┴───────────────────────┤
│              TtsEngineSelector               │
│  Routes to Android or Local based on config  │
├─────────────────────────────────────────────┤
│         LocalVoiceManager (NEW)              │
│  - Model registry & download                 │
│  - ONNX Runtime initialization               │
│  - Audio playback via AudioTrack             │
└─────────────────────────────────────────────┘
```

### Key Components

1. **`LocalTtsEngine`** - Implements `TtsEngine`, wraps Sherpa-ONNX
2. **`LocalVoiceManager`** - Manages ONNX models, voice registry, download
3. **`OnnxModelDownloader`** - Downloads model files from GitHub releases
4. **`VoiceModelRegistry`** - Curated list of available models with metadata
5. **`LocalVoiceSettingsScreen`** - UI for browsing/selecting local voices

### Model Storage

```
/data/data/com.hourlyvoiceclock/files/local_tts/
├── models/
│   ├── piper_en_us_amy_medium/
│   │   ├── model.onnx
│   │   └── model.onnx.json
│   └── piper_en_gb_alba_medium/
│       ├── model.onnx
│       └── model.onnx.json
└── voices.json  (cached voice metadata)
```

### Integration Points

- **`TtsEngineSelector`** - Add `isLocalEngine()` check
- **`AppSettings`** - New `useLocalTts: Boolean` and `selectedLocalVoice: String?`
- **`TimeAnnouncer`** - Routes to local engine when enabled
- **`VoiceSettingsScreen`** - Add "Local Voices" section with download UI

## Implementation Phases

### Phase 1: Core Engine (Scaffold)
- [ ] Add Sherpa-ONNX dependency
- [ ] Create `LocalTtsEngine` implementing `TtsEngine`
- [ ] Create `LocalVoiceManager` with model loading
- [ ] Create `VoiceModelRegistry` with curated voice list
- [ ] Wire into `DependenciesProvider`

### Phase 2: Model Management
- [ ] Create `OnnxModelDownloader` with progress tracking
- [ ] Add model storage/cleanup utilities
- [ ] Background download with notification

### Phase 3: UI Integration
- [ ] Local voice browser in VoiceSettingsScreen
- [ ] Download progress indicators
- [ ] Model size display and delete option

### Phase 4: Polish
- [ ] Audio format optimization (16kHz mono PCM)
- [ ] Warm-up on first use to avoid latency on first announcement
- [ ] Battery/memory impact monitoring

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Model download size (~20-50MB per voice) | Show size before download, allow delete |
| First-inference latency (~200-500ms) | Warm-up engine in background on app start |
| Memory usage (~50-100MB during inference) | Release model after each utterance |
| ONNX Runtime AAR adds ~15MB to APK | Use ABI split (only ship arm64) |
| Model compatibility | Test with Piper v1.x models, pin versions |

## Dependencies

The Sherpa-ONNX AAR is downloaded from GitHub releases and included as a local file dependency:

```kotlin
// app/build.gradle.kts
implementation(files("libs/sherpa-onnx-1.13.2.aar"))
```

Download the AAR:
```bash
mkdir -p app/libs
curl -L -o app/libs/sherpa-onnx-1.13.2.aar \
  "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.2/sherpa-onnx-1.13.2.aar"
```

The `app/libs/` directory is gitignored (55MB binary).
