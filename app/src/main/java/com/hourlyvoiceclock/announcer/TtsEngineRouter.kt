package com.hourlyvoiceclock.announcer

import android.util.Log
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry

/**
 * Picks the right [TtsEngine] for a given announcement.
 *
 * The user can now choose either:
 *  - a system TTS voice (Google, eSpeak NG, etc.) — handled by the
 *    primary Android TTS engine, or
 *  - a downloaded on-device voice (Piper via Sherpa-ONNX) — handled
 *    by [LocalTtsEngine] with the model id stored in
 *    [AppSettings.selectedLocalModelId].
 *
 * Routing is a property of the *announcement*, not of construction
 * time, so this object is a small policy component that the
 * [TimeAnnouncer] consults each time it speaks. It holds a
 * reference to the primary engine (always present) and a
 * best-effort reference to the local engine (only constructed
 * when at least one local model has been downloaded, to avoid the
 * Sherpa-ONNX native init cost on cold start).
 */
class TtsEngineRouter(
    private val primaryEngine: TtsEngine,
    private val localEngineFactory: () -> TtsEngine,
    private val isKnownModel: (modelId: String) -> Boolean = {
        VoiceModelRegistry.getVoiceById(it) != null
    },
    private val isLocalModelDownloaded: (modelId: String) -> Boolean = { true }
) {

    @Volatile
    private var localEngineInstance: TtsEngine? = null

    /**
     * Returns the engine that should speak this announcement, and
     * ensures it is initialized for the user's selection.
     *
     * The local engine is created lazily on first use, kept alive
     * for the rest of the process, and re-used across announcements
     * so the ONNX model stays loaded in memory between hourly
     * triggers.
     */
    fun resolveFor(settings: AppSettings): TtsEngine {
        val localId = settings.selectedLocalModelId
        if (localId.isNullOrBlank()) {
            return primaryEngine
        }
        // Validate the model id is real and present on disk; otherwise
        // fall through to the primary engine. We do not want a stale id
        // (e.g. a model the user deleted) to leave the announcer mute
        // or force a pointless local-engine init.
        if (!isKnownModel(localId)) {
            return primaryEngine
        }
        if (!isLocalModelDownloaded(localId)) {
            return primaryEngine
        }
        val local = localEngineInstance ?: synchronized(this) {
            localEngineInstance ?: localEngineFactory().also { localEngineInstance = it }
        }
        // Switch the local engine to the requested model. The local
        // engine handles the no-op when the model is already current.
        val ok = local.setVoice(localId, settings.selectedLocale ?: "en-US")
        if (!ok) {
            Log.w(TAG, "LocalTtsEngine.setVoice($localId) failed; falling back to primary engine")
            return primaryEngine
        }
        return local
    }

    /**
     * Returns the local engine if it has been instantiated, else null.
     * Used by the voice-settings screen to read its current state
     * without forcing construction.
     */
    fun localEngineOrNull(): TtsEngine? = localEngineInstance

    companion object {
        private const val TAG = "TtsEngineRouter"
    }
}
