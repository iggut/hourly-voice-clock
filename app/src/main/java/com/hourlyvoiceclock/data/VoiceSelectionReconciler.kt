package com.hourlyvoiceclock.data

import android.util.Log
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineSelector
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry
import kotlinx.coroutines.flow.first

/**
 * Clears stale voice-related settings at app startup so upgrades and
 * deleted models do not leave the announcer on a silent/fallback path
 * while DataStore still points at missing engines, voices, or local models.
 */
class VoiceSelectionReconciler(
    private val loadSettings: suspend () -> AppSettings,
    private val updateSettings: suspend (transform: (AppSettings) -> AppSettings) -> Unit,
    private val selectEnginePackage: suspend () -> String?,
    private val initializeEngine: suspend (enginePackage: String?) -> Unit,
    private val listSystemVoices: () -> List<VoiceInfo>,
    private val isKnownLocalModel: (modelId: String) -> Boolean = {
        VoiceModelRegistry.getVoiceById(it) != null
    },
    private val isLocalModelDownloaded: (modelId: String) -> Boolean
) {

    constructor(
        settings: SettingsRepository,
        engineSelector: TtsEngineSelector,
        ttsEngine: TtsEngine,
        isLocalModelDownloaded: (modelId: String) -> Boolean
    ) : this(
        loadSettings = { settings.settings.first() },
        updateSettings = { transform -> settings.update(transform) },
        selectEnginePackage = { engineSelector.select() },
        initializeEngine = { pkg -> ttsEngine.initialize(pkg) },
        listSystemVoices = { ttsEngine.getVoices() },
        isLocalModelDownloaded = isLocalModelDownloaded
    )

    suspend fun reconcile() {
        val enginePackage = selectEnginePackage()
        initializeEngine(enginePackage)

        val current = loadSettings()
        var next = current

        val localId = current.selectedLocalModelId
        if (!localId.isNullOrBlank()) {
            val onDisk = isKnownLocalModel(localId) && isLocalModelDownloaded(localId)
            if (!onDisk) {
                Log.w(TAG, "Clearing stale selectedLocalModelId=$localId (missing or invalid on disk)")
                next = next.copy(selectedLocalModelId = null)
            }
        }

        val savedVoice = next.selectedVoiceName
        if (!savedVoice.isNullOrBlank()) {
            val savedLocale = next.selectedLocale
            val voices = listSystemVoices()
            val stillExists = voices.any {
                it.name == savedVoice && (savedLocale.isNullOrBlank() || it.localeTag == savedLocale)
            }
            if (!stillExists) {
                Log.w(TAG, "Clearing stale selectedVoiceName=$savedVoice")
                next = next.copy(
                    selectedVoiceName = null,
                    selectedLocale = null,
                    selectedVoicePresetId = null
                )
            }
        }

        if (next != current) {
            updateSettings { next }
        }
    }

    companion object {
        private const val TAG = "VoiceSelectionReconciler"
    }
}
