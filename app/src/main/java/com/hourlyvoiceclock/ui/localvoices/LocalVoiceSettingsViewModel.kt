package com.hourlyvoiceclock.ui.localvoices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.tts.local.DownloadException
import com.hourlyvoiceclock.tts.local.LocalVoiceRepository
import com.hourlyvoiceclock.tts.local.VoiceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LocalVoiceListFilter {
    ALL,
    INSTALLED
}

class LocalVoiceSettingsViewModel(
    application: Application,
    private val repository: LocalVoiceRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    val downloadedModels: StateFlow<List<VoiceModel>> = repository.downloadedModels

    val downloadProgressByModelId: StateFlow<Map<String, Float>> = repository.downloadProgressByModelId

    val selectedLocalModelId: StateFlow<String?> = settingsRepository.settings
        .map { it.selectedLocalModelId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _previewingModelId = MutableStateFlow<String?>(null)
    val previewingModelId: StateFlow<String?> = _previewingModelId.asStateFlow()

    private val _listFilter = MutableStateFlow(LocalVoiceListFilter.ALL)
    val listFilter: StateFlow<LocalVoiceListFilter> = _listFilter.asStateFlow()

    /**
     * Per-model errors from downloads (queue) plus preview failures.
     */
    private val _localErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errorsByModelId: StateFlow<Map<String, String>> =
        kotlinx.coroutines.flow.combine(
            repository.downloadErrorsByModelId,
            _localErrors
        ) { downloadErrors, local ->
            downloadErrors + local
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        refreshDownloadedModels()
    }

    fun setListFilter(filter: LocalVoiceListFilter) {
        _listFilter.value = filter
    }

    fun refreshDownloadedModels() {
        viewModelScope.launch {
            repository.refreshDownloadedModels()
        }
    }

    fun downloadModel(model: VoiceModel) {
        _localErrors.value = _localErrors.value - model.id
        val started = repository.enqueueDownload(model)
        if (!started) {
            Log.d(TAG, "Download not started for ${model.id} (already active or installed)")
        }
    }

    fun cancelDownload(modelId: String) {
        repository.cancelDownload(modelId)
    }

    fun clearError(modelId: String) {
        _localErrors.value = _localErrors.value - modelId
    }

    fun deleteModel(model: VoiceModel) {
        viewModelScope.launch {
            if (_previewingModelId.value == model.id) {
                repository.stopPreview()
                _previewingModelId.value = null
            }
            repository.cancelDownload(model.id)
            repository.deleteModel(model)
            settingsRepository.update { settings ->
                if (settings.selectedLocalModelId == model.id) {
                    settings.copy(selectedLocalModelId = null)
                } else {
                    settings
                }
            }
            refreshDownloadedModels()
        }
    }

    fun previewVoice(model: VoiceModel) {
        if (downloadProgressByModelId.value.containsKey(model.id)) {
            _localErrors.value = _localErrors.value + (
                model.id to getApplication<Application>().getString(R.string.download_failed)
            )
            return
        }
        val current = _previewingModelId.value
        if (current != null) {
            repository.stopPreview()
            _previewingModelId.value = null
            if (current == model.id) return
        }

        viewModelScope.launch {
            _previewingModelId.value = model.id
            repository.preview(model) { message ->
                _localErrors.value = _localErrors.value + (model.id to message)
            }
            if (_previewingModelId.value == model.id) {
                _previewingModelId.value = null
            }
        }
    }

    fun stopSpeaking() {
        repository.stopPreview()
        _previewingModelId.value = null
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
            val deps = (application as DependenciesProvider).dependencies
            return LocalVoiceSettingsViewModel(
                application,
                deps.localVoiceRepository,
                deps.settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
