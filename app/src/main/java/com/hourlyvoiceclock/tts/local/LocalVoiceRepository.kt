package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Seam for downloaded on-device (Piper/Sherpa-ONNX) voice models.
 *
 * The repository is the single source of truth for which local models are
 * present, and it isolates the heavy [LocalTtsEngine] lifecycle from the
 * UI layer. Both [com.hourlyvoiceclock.ui.voicesettings.VoiceSettingsViewModel]
 * and [com.hourlyvoiceclock.ui.localvoices.LocalVoiceSettingsViewModel]
 * observe the same [downloadedModels] flow through this interface.
 */
interface LocalVoiceRepository {
    val downloadedModels: StateFlow<List<VoiceModel>>

    suspend fun refreshDownloadedModels()

    suspend fun downloadModel(model: VoiceModel, onProgress: (Float) -> Unit): Result<java.io.File>

    suspend fun deleteModel(model: VoiceModel)

    /**
     * Preview a downloaded model by initializing the local engine and speaking
     * a short sample. Errors are delivered to [onError] on the calling thread.
     */
    suspend fun preview(model: VoiceModel, onError: (String) -> Unit)

    fun stopPreview()
}

class DefaultLocalVoiceRepository(context: Context) : LocalVoiceRepository {

    private val engine = LocalTtsEngine(context.applicationContext)
    private val downloader = engine.getModelDownloader()

    private val _downloadedModels = MutableStateFlow<List<VoiceModel>>(emptyList())
    override val downloadedModels: StateFlow<List<VoiceModel>> = _downloadedModels.asStateFlow()

    override suspend fun refreshDownloadedModels() {
        val models = withContext(Dispatchers.IO) { downloader.getDownloadedModels() }
        _downloadedModels.value = models
    }

    override suspend fun downloadModel(model: VoiceModel, onProgress: (Float) -> Unit): Result<java.io.File> {
        return withContext(Dispatchers.IO) {
            downloader.downloadModel(model, onProgress)
        }
    }

    override suspend fun deleteModel(model: VoiceModel) {
        withContext(Dispatchers.IO) { downloader.deleteModel(model) }
    }

    override suspend fun preview(model: VoiceModel, onError: (String) -> Unit) {
        engine.stop()
        try {
            val initialized = withContext(Dispatchers.IO) { engine.initialize(model.id) }
            if (!initialized) {
                onError("Preview failed: model files are not loadable. Try deleting and re-downloading.")
                return
            }
            engine.speakAsync("Hello from Hourly Voice Clock") { success ->
                if (!success) {
                    onError("Preview failed: TTS engine could not synthesize audio")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Preview crashed for ${model.id}", t)
            onError(t.message ?: t.javaClass.simpleName)
        }
    }

    override fun stopPreview() {
        engine.stop()
    }

    companion object {
        private const val TAG = "LocalVoiceRepository"
    }
}
