## Learned User Preferences

- Prefer Special Voices as part of the unified voice list with All / Male / Female / Special filters, not a separate sloppy carousel; colored accents for emphasis are welcome.
- App updates and installs over older versions must keep working; treat upgrade/migration failures as high priority.
- When improving voice UX, cover both Special Voices and Local AI Voices together unless scoped otherwise.
- Often asks to review/merge open non-duplicate PRs and sync local `main` afterward.

## Learned Workspace Facts

- Android Kotlin/Compose app (Hourly Voice Clock) that announces the time via system TTS and optional on-device Piper Local AI voices; issues live at github.com/iggut/hourly-voice-clock.
- Theme surfaces use semi-transparent glassmorphism colors; AlertDialogs must use opaque `DialogSurface` helpers from `DialogColors.kt`, not `colorScheme.surface`.
- Special Voices are system-TTS presets on Voice Settings; Local AI Voices are downloadable Piper models. Selecting a system/special voice clears `selectedLocalModelId`, and deleting the active local model clears that selection.
- Startup settings migration clamps pitch/rate, recovers corrupted DataStore, and clears missing engines/voices so upgrades from older installs stay safe.
- Gradle/AGP builds need JDK 21 on this machine (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk`); the default Java 25 toolchain is not supported yet.
- In-app updates use GitHub releases with signature checks; sideloaded APK upgrades only succeed when signed with the same key as the installed app.
