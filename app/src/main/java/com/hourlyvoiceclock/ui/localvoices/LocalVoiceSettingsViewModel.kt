package com.hourlyvoiceclock.ui.localvoices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.tts.local.DownloadException
import com.hourlyvoiceclock.tts.local.LocalTtsEngine
import com.hourlyvoiceclock.tts.local.OnnxModelDownloader
import com.hourlyvoiceclock.tts.local.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalVoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val localEngine = LocalTtsEngine(application)
    private val downloader = localEngine.getModelDownloader()

    private val _downloadedModels = MutableStateFlow<List<VoiceModel>>(emptyList())
    val downloadedModels: StateFlow<List<VoiceModel>> = _downloadedModels.asStateFlow()

    private val _downloadingModel = MutableStateFlow<VoiceModel?>(null)
    val downloadingModel: StateFlow<VoiceModel?> = _downloadingModel.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /**
     * Per-model error message to surface inline in the card so the user
     * sees a failed download instead of a silent button reset. Cleared
     * when the user starts a new download for the same model.
     */
    private val _errorsByModelId = MutableStateFlow<Map<String, String>>(emptyMap())
    val errorsByModelId: StateFlow<Map<String, String>> = _errorsByModelId.asStateFlow()

    init {
        refreshDownloadedModels()
    }

    fun refreshDownloadedModels() {
        viewModelScope.launch {
            val models = withContext(Dispatchers.IO) { downloader.getDownloadedModels() }
            _downloadedModels.value = models
        }
    }

    fun downloadModel(model: VoiceModel) {
        if (_downloadingModel.value != null) return
        // Clear any prior error for this model.
        _errorsByModelId.value = _errorsByModelId.value - model.id

        _downloadingModel.value = model
        _downloadProgress.value = 0f

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                downloader.downloadModel(model) { progress ->
                    _downloadProgress.value = progress
                }
            }

            _downloadingModel.value = null
            _downloadProgress.value = 0f

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Downloaded model: ${model.id}")
                    refreshDownloadedModels()
                },
                onFailure = { throwable ->
                    val message = humanize(throwable)
                    Log.e(TAG, "Failed to download model: ${model.id}", throwable)
                    _errorsByModelId.value = _errorsByModelId.value + (model.id to message)
                    refreshDownloadedModels()
                }
            )
        }
    }

    fun clearError(modelId: String) {
        if (_errorsByModelId.value.containsKey(modelId)) {
            _errorsByModelId.value = _errorsByModelId.value - modelId
        }
    }

    fun deleteModel(model: VoiceModel) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { downloader.deleteModel(model) }
            refreshDownloadedModels()
        }
    }

    fun previewVoice(model: VoiceModel) {
        if (_isSpeaking.value) {
            localEngine.stop()
            _isSpeaking.value = false
            return
        }

        viewModelScope.launch {
            try {
                val initialized = withContext(Dispatchers.IO) { localEngine.initialize(model.id) }
                if (!initialized) {
                    Log.w(TAG, "Preview failed: engine.initialize returned false for ${model.id}")
                    _errorsByModelId.value = _errorsByModelId.value +
                        (model.id to "Preview failed: model files are not loadable. Try deleting and re-downloading.")
                    return@launch
                }
                _isSpeaking.value = true
                localEngine.speakAsync(
                    "It is now ${
                        java.time.LocalTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                        )
                    }"
                ) { success ->
                    _isSpeaking.value = false
                    Log.d(TAG, "Preview finished: success=$success")
                    if (!success) {
                        _errorsByModelId.value = _errorsByModelId.value +
                            (model.id to "Preview failed: TTS engine could not synthesize audio")
                    }
                }
            } catch (t: Throwable) {
                _isSpeaking.value = false
                Log.e(TAG, "Preview crashed for ${model.id}", t)
                _errorsByModelId.value = _errorsByModelId.value +
                    (model.id to (t.message ?: t.javaClass.simpleName))
            }
        }
    }

    fun stopSpeaking() {
        localEngine.stop()
        _isSpeaking.value = false
    }

    fun getLocalEngine(): LocalTtsEngine = localEngine

    override fun onCleared() {
        super.onCleared()
        localEngine.shutdown()
    }

    private fun humanize(t: Throwable): String = when (t) {
        is DownloadException -> t.message ?: "Download failed"
        else -> t.message ?: t.javaClass.simpleName
    }

    companion object {
        private const val TAG = "LocalVoiceVM"
    }
}

class LocalVoiceSettingsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalVoiceSettingsViewModel::class.java)) {
            return LocalVoiceSettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
