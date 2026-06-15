package com.hourlyvoiceclock.ui.localvoices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        refreshDownloadedModels()
    }

    fun refreshDownloadedModels() {
        viewModelScope.launch {
            _downloadedModels.value = downloader.getDownloadedModels()
        }
    }

    fun downloadModel(model: VoiceModel) {
        if (_downloadingModel.value != null) return

        viewModelScope.launch {
            _downloadingModel.value = model
            _downloadProgress.value = 0f

            val result = downloader.downloadModel(model) { progress ->
                _downloadProgress.value = progress
            }

            _downloadingModel.value = null
            _downloadProgress.value = 0f

            if (result.isSuccess) {
                Log.d(TAG, "Downloaded model: ${model.id}")
                refreshDownloadedModels()
            } else {
                Log.e(TAG, "Failed to download model: ${model.id}", result.exceptionOrNull())
            }
        }
    }

    fun deleteModel(model: VoiceModel) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                downloader.deleteModel(model)
            }
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
            val initialized = localEngine.initialize(model.id)
            if (initialized) {
                _isSpeaking.value = true
                localEngine.speakAsync("It is now ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))}") { success ->
                    _isSpeaking.value = false
                    Log.d(TAG, "Preview finished: success=$success")
                }
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

    companion object {
        private const val TAG = "LocalVoiceVM"
    }
}
