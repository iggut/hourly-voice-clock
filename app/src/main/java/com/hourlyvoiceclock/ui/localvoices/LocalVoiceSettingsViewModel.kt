package com.hourlyvoiceclock.ui.localvoices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.tts.local.DownloadException
import com.hourlyvoiceclock.tts.local.LocalVoiceRepository
import com.hourlyvoiceclock.tts.local.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalVoiceSettingsViewModel(
    application: Application,
    private val repository: LocalVoiceRepository
) : AndroidViewModel(application) {

    val downloadedModels: StateFlow<List<VoiceModel>> = repository.downloadedModels

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
            repository.refreshDownloadedModels()
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
                repository.downloadModel(model) { progress ->
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
            withContext(Dispatchers.IO) { repository.deleteModel(model) }
            refreshDownloadedModels()
        }
    }

    fun previewVoice(model: VoiceModel) {
        if (_isSpeaking.value) {
            repository.stopPreview()
            _isSpeaking.value = false
            return
        }

        viewModelScope.launch {
            _isSpeaking.value = true
            repository.preview(model) { message ->
                _isSpeaking.value = false
                _errorsByModelId.value = _errorsByModelId.value + (model.id to message)
            }
        }
    }

    fun stopSpeaking() {
        repository.stopPreview()
        _isSpeaking.value = false
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
            val repo = (application as DependenciesProvider).dependencies.localVoiceRepository
            return LocalVoiceSettingsViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
