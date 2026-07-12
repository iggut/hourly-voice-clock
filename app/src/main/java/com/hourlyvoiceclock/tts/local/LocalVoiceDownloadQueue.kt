package com.hourlyvoiceclock.tts.local

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/**
 * Parallel download queue for Piper models. At most [maxConcurrency]
 * downloads run at once; additional enqueues wait on the semaphore.
 * The same [VoiceModel.id] is never downloaded twice concurrently.
 */
class LocalVoiceDownloadQueue(
    private val scope: CoroutineScope,
    private val downloader: OnnxModelDownloader,
    private val maxConcurrency: Int = 2,
    private val onFinished: suspend () -> Unit = {},
    private val download: suspend (VoiceModel, (Float) -> Unit) -> Result<File> = { model, onProgress ->
        downloader.downloadModel(model, onProgress)
    },
    private val isDownloaded: (VoiceModel) -> Boolean = { downloader.isModelDownloaded(it) }
) {
    private val semaphore = Semaphore(maxConcurrency)
    private val jobs = mutableMapOf<String, Job>()

    private val _progressByModelId = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressByModelId: StateFlow<Map<String, Float>> = _progressByModelId.asStateFlow()

    private val _errorsByModelId = MutableStateFlow<Map<String, String>>(emptyMap())
    val errorsByModelId: StateFlow<Map<String, String>> = _errorsByModelId.asStateFlow()

    fun isActive(modelId: String): Boolean = jobs[modelId]?.isActive == true

    fun enqueue(model: VoiceModel): Boolean {
        synchronized(jobs) {
            if (jobs[model.id]?.isActive == true) return false
            if (isDownloaded(model)) return false

            _errorsByModelId.update { it - model.id }
            _progressByModelId.update { it + (model.id to 0f) }

            val job = scope.launch {
                try {
                    semaphore.withPermit {
                        val result = download(model) { progress ->
                            _progressByModelId.update { it + (model.id to progress) }
                        }
                        result.fold(
                            onSuccess = {
                                Log.d(TAG, "Downloaded ${model.id}")
                                _errorsByModelId.update { it - model.id }
                            },
                            onFailure = { t ->
                                Log.e(TAG, "Download failed for ${model.id}", t)
                                _errorsByModelId.update {
                                    it + (model.id to (t.message ?: t.javaClass.simpleName))
                                }
                            }
                        )
                    }
                } finally {
                    _progressByModelId.update { it - model.id }
                    synchronized(jobs) { jobs.remove(model.id) }
                    onFinished()
                }
            }
            jobs[model.id] = job
            return true
        }
    }

    fun cancel(modelId: String) {
        val job = synchronized(jobs) { jobs.remove(modelId) } ?: return
        job.cancel()
        _progressByModelId.update { it - modelId }
        VoiceModelRegistry.getVoiceById(modelId)?.let { model ->
            if (!isDownloaded(model)) {
                downloader.deleteModel(model)
            }
        }
    }

    companion object {
        private const val TAG = "LocalVoiceDownloadQueue"
    }
}
