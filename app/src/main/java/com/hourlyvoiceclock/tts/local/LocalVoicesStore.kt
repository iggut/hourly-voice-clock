package com.hourlyvoiceclock.tts.local

import com.hourlyvoiceclock.tts.local.VoiceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped cache of the downloaded on-device Piper voices.
 *
 * Both the [com.hourlyvoiceclock.ui.voicesettings.VoiceSettingsViewModel]
 * and [com.hourlyvoiceclock.ui.localvoices.LocalVoiceSettingsViewModel]
 * need to know which local models are present, but they live in
 * different Android scopes (different `ViewModelProvider`s) and
 * cannot share a normal ViewModel-scoped flow. This singleton is
 * the shared source of truth.
 *
 * The local-voice screen is authoritative: every time it finishes
 * a download or delete, it pushes the new list here. The voice
 * screen reads it on entry and after the user navigates back.
 */
class LocalVoicesStore {

    private val _downloadedModels = MutableStateFlow<List<VoiceModel>>(emptyList())
    val downloadedModels: StateFlow<List<VoiceModel>> = _downloadedModels.asStateFlow()

    fun setDownloadedModels(models: List<VoiceModel>) {
        _downloadedModels.value = models
    }
}
