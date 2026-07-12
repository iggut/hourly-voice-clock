package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
import com.hourlyvoiceclock.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
    val downloadProgressByModelId: StateFlow<Map<String, Float>>
    val downloadErrorsByModelId: StateFlow<Map<String, String>>

    suspend fun refreshDownloadedModels()

    fun enqueueDownload(model: VoiceModel): Boolean

    fun cancelDownload(modelId: String)

    /** Direct download used by tests; UI should prefer [enqueueDownload]. */
    suspend fun downloadModel(model: VoiceModel, onProgress: (Float) -> Unit): Result<java.io.File>

    suspend fun deleteModel(model: VoiceModel)

    /**
     * Preview a downloaded model by initializing the local engine and speaking
     * a short sample. Suspends until playback finishes (or fails).
     * Errors are delivered to [onError] before the suspend returns.
     */
    suspend fun preview(model: VoiceModel, onError: (String) -> Unit = {})

    fun stopPreview()
}

class DefaultLocalVoiceRepository(context: Context) : LocalVoiceRepository {

    private val appContext = context.applicationContext
    private val engine = LocalTtsEngine(appContext)
    private val downloader = engine.getModelDownloader()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val queue = LocalVoiceDownloadQueue(
        scope = scope,
        downloader = downloader,
        maxConcurrency = 2,
        onFinished = { refreshDownloadedModels() }
    )

    private val _downloadedModels = kotlinx.coroutines.flow.MutableStateFlow<List<VoiceModel>>(emptyList())
    override val downloadedModels: StateFlow<List<VoiceModel>> = _downloadedModels

    override val downloadProgressByModelId: StateFlow<Map<String, Float>> = queue.progressByModelId
    override val downloadErrorsByModelId: StateFlow<Map<String, String>> = queue.errorsByModelId

    override suspend fun refreshDownloadedModels() {
        val models = withContext(Dispatchers.IO) { downloader.getDownloadedModels() }
        _downloadedModels.value = models
    }

    override fun enqueueDownload(model: VoiceModel): Boolean = queue.enqueue(model)

    override fun cancelDownload(modelId: String) = queue.cancel(modelId)

    override suspend fun downloadModel(model: VoiceModel, onProgress: (Float) -> Unit): Result<java.io.File> {
        return withContext(Dispatchers.IO) {
            downloader.downloadModel(model, onProgress)
        }
    }

    override suspend fun deleteModel(model: VoiceModel) {
        withContext(Dispatchers.IO) { downloader.deleteModel(model) }
    }

    override suspend fun preview(model: VoiceModel, onError: (String) -> Unit) {
        if (queue.isActive(model.id)) {
            onError(appContext.getString(R.string.download_failed))
            return
        }
        engine.stop()
        try {
            val initialized = withContext(Dispatchers.IO) { engine.initialize(model.id) }
            if (!initialized) {
                onError(appContext.getString(R.string.preview_failed_not_loadable))
                return
            }
            suspendCancellableCoroutine { cont ->
                engine.speakAsync("Hello from Hourly Voice Clock") { success ->
                    if (!success) {
                        onError(appContext.getString(R.string.preview_failed_synthesize))
                    }
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }
                cont.invokeOnCancellation { engine.stop() }
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
