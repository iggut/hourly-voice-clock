package com.hourlyvoiceclock.ui.localvoices

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.hourlyvoiceclock.data.SettingsRepository
import com.hourlyvoiceclock.tts.local.LocalVoiceRepository
import com.hourlyvoiceclock.tts.local.VoiceModel
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocalVoiceSettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var settings: SettingsRepository
    private lateinit var repository: FakeLocalVoiceRepository
    private lateinit var viewModel: LocalVoiceSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        settings = SettingsRepository(app)
        repository = FakeLocalVoiceRepository()
        viewModel = LocalVoiceSettingsViewModel(app, repository, settings)
    }

    @After
    fun tearDown() = runBlocking {
        Dispatchers.resetMain()
        settings.update {
            it.copy(selectedLocalModelId = null)
        }
    }

    @Test
    fun `deleteModel clears selectedLocalModelId when deleting active voice`() = runTest(dispatcher) {
        val model = sampleModel("voice-a")
        repository.markDownloaded(model)
        settings.update { it.copy(selectedLocalModelId = model.id) }
        assertEquals(model.id, settings.settings.first().selectedLocalModelId)

        viewModel.deleteModel(model)
        advanceUntilIdle()

        val cleared = settings.settings.first { it.selectedLocalModelId == null }
        assertNull(cleared.selectedLocalModelId)
        assertEquals(false, repository.downloadedModels.value.any { it.id == model.id })
    }

    @Test
    fun `deleteModel preserves selectedLocalModelId for other models`() = runTest(dispatcher) {
        val active = sampleModel("voice-active")
        val other = sampleModel("voice-other")
        repository.markDownloaded(active)
        repository.markDownloaded(other)
        settings.update { it.copy(selectedLocalModelId = active.id) }

        viewModel.deleteModel(other)
        advanceUntilIdle()

        val retained = settings.settings.first { it.selectedLocalModelId == active.id }
        assertEquals(active.id, retained.selectedLocalModelId)
    }

    @Test
    fun `previewVoice clears previewingModelId after completion`() = runTest(dispatcher) {
        val model = sampleModel("voice-preview")
        repository.markDownloaded(model)

        viewModel.previewVoice(model)
        advanceUntilIdle()

        assertNull(viewModel.previewingModelId.value)
    }

    private fun sampleModel(id: String): VoiceModel {
        val base = VoiceModelRegistry.availableVoices.first()
        return base.copy(id = id)
    }

    private class FakeLocalVoiceRepository : LocalVoiceRepository {
        private val _downloaded = MutableStateFlow<List<VoiceModel>>(emptyList())
        override val downloadedModels: StateFlow<List<VoiceModel>> = _downloaded.asStateFlow()

        private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
        override val downloadProgressByModelId: StateFlow<Map<String, Float>> = _progress.asStateFlow()

        private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
        override val downloadErrorsByModelId: StateFlow<Map<String, String>> = _errors.asStateFlow()

        private val deleted = mutableSetOf<String>()

        fun markDownloaded(model: VoiceModel) {
            _downloaded.value = _downloaded.value + model
        }

        override suspend fun refreshDownloadedModels() {
            _downloaded.value = _downloaded.value.filterNot { it.id in deleted }
        }

        override fun enqueueDownload(model: VoiceModel): Boolean = false

        override fun cancelDownload(modelId: String) = Unit

        override suspend fun downloadModel(
            model: VoiceModel,
            onProgress: (Float) -> Unit
        ): Result<File> = Result.success(File("/tmp/${model.id}"))

        override suspend fun deleteModel(model: VoiceModel) {
            deleted += model.id
            _downloaded.value = _downloaded.value.filterNot { it.id == model.id }
        }

        override suspend fun preview(model: VoiceModel, onError: (String) -> Unit) = Unit

        override fun stopPreview() = Unit
    }
}
