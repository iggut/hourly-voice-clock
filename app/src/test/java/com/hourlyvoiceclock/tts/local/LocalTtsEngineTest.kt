package com.hourlyvoiceclock.tts.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.tts.VoiceProfile
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocalTtsEngineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val fakeModel = VoiceModel(
        id = "piper_en_us_amy_medium",
        displayNameRes = com.hourlyvoiceclock.R.string.voice_piper_en_us_amy_medium_name,
        descriptionRes = com.hourlyvoiceclock.R.string.voice_piper_en_us_amy_medium_desc,
        language = "en-US",
        sizeBytes = 100,
        onnxDownloadUrl = "",
        onnxJsonDownloadUrl = "",
        onnxFileName = "model.onnx",
        onnxJsonFileName = "model.onnx.json"
    )

    @Test
    fun `initialize returns false for unknown model`() = runTest {
        val engine = createEngine(modelLookup = { null })

        assertFalse(engine.initialize("unknown"))
    }

    @Test
    fun `initialize succeeds when model is prepared`() = runTest {
        val engine = createEngine()

        assertTrue(engine.initialize(fakeModel.id))
        assertTrue(engine.isAvailable())
        assertEquals(fakeModel.id, engine.getCurrentEnginePackage())
    }

    @Test
    fun `setVoice switches to a different model`() = runTest {
        val otherModel = fakeModel.copy(id = "piper_en_us_john")
        val engine = createEngine(modelLookup = { id ->
            when (id) {
                fakeModel.id -> fakeModel
                otherModel.id -> otherModel
                else -> null
            }
        })
        engine.initialize(fakeModel.id)

        assertTrue(engine.setVoice(otherModel.id, "en-US"))
        assertEquals(otherModel.id, engine.getCurrentEnginePackage())
    }

    @Test
    fun `configure loads the requested model and applies the audio channel`() = runTest {
        val engine = createEngine()

        val result = engine.configure(
            VoiceProfile(
                voiceName = fakeModel.id,
                localeTag = "en-US",
                localModelId = fakeModel.id,
                pitch = 1.0f,
                speechRate = 1.0f,
                audioChannel = AudioChannel.CALL
            )
        )

        assertTrue(result)
        assertEquals(fakeModel.id, engine.getCurrentEnginePackage())
    }

    @Test
    fun `configure returns false when no voice or locale is provided`() = runTest {
        val engine = createEngine()

        val result = engine.configure(
            VoiceProfile(
                voiceName = null,
                localeTag = null,
                localModelId = null,
                pitch = 1.0f,
                speechRate = 1.0f,
                audioChannel = AudioChannel.MEDIA
            )
        )

        assertFalse(result)
    }

    @Test
    fun `speakAsync completes successfully when synthesis produces audio`() = runTest {
        val fakeAudio = GeneratedAudio(FloatArray(100) { 0.1f }, 22050)
        val fakeSynth = FakeSynthesizer(fakeAudio)
        val engine = createEngine(synthesizer = fakeSynth)

        engine.initialize(fakeModel.id)

        var completed = false
        var success = false
        engine.speakAsync("hello") { result ->
            completed = true
            success = result
        }
        advanceUntilIdle()

        assertTrue(completed)
        assertTrue(success)
    }

    @Test
    fun `speakAsync fails when synthesizer returns null`() = runTest {
        val engine = createEngine(synthesizer = FakeSynthesizer(null))

        engine.initialize(fakeModel.id)

        var success = true
        engine.speakAsync("hello") { success = it }
        advanceUntilIdle()

        assertFalse(success)
    }

    @Test
    fun `stop cancels ongoing synthesis`() = runTest {
        val fakeAudio = GeneratedAudio(FloatArray(100) { 0.1f }, 22050)
        val engine = createEngine(synthesizer = FakeSynthesizer(fakeAudio))

        engine.initialize(fakeModel.id)

        var success = true
        engine.speakAsync("hello") { success = it }
        engine.stop()
        advanceUntilIdle()

        assertFalse(success)
    }

    @Test
    fun `shutdown clears state`() = runTest {
        val engine = createEngine()
        engine.initialize(fakeModel.id)

        engine.shutdown()

        assertFalse(engine.isAvailable())
        assertEquals(null, engine.getCurrentEnginePackage())
    }

    private fun TestScope.createEngine(
        modelLookup: (String) -> VoiceModel? = { id -> if (id == fakeModel.id) fakeModel else null },
        synthesizer: LocalTtsSynthesizer = FakeSynthesizer(GeneratedAudio(FloatArray(100) { 0.1f }, 22050))
    ): LocalTtsEngine {
        return LocalTtsEngine(
            modelLoader = FakeModelLoader(context),
            synthesizerFactory = { synthesizer },
            audioPlayer = FakeAudioPlayer(),
            downloader = FakeDownloader(context, listOf(fakeModel)),
            modelLookup = modelLookup,
            coroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        )
    }
}

private class FakeModelLoader(context: Context) : LocalTtsModelLoader(
    context = context,
    downloader = object : OnnxModelDownloader(context) {
        override fun isModelDownloaded(model: VoiceModel): Boolean = true
    }
) {
    override fun prepareModel(model: VoiceModel): Result<PreparedModel> {
        return Result.success(
            PreparedModel(
                modelDir = File("/tmp/model"),
                modelFileName = "model.onnx",
                espeakDataDir = File("/tmp/espeak")
            )
        )
    }
}

private class FakeSynthesizer(private val audio: GeneratedAudio?) : LocalTtsSynthesizer {
    override val sampleRate: Int = 22050
    var released = false

    override fun generate(text: String, speed: Float): GeneratedAudio? = audio

    override fun release() {
        released = true
    }
}

private class FakeAudioPlayer : LocalTtsAudioPlayer {
    var played = false
    override suspend fun play(samples: FloatArray, sampleRate: Int, channel: AudioChannel) {
        played = true
    }
}

private class FakeDownloader(context: Context, private val models: List<VoiceModel>) :
    OnnxModelDownloader(context) {
    override fun isModelDownloaded(model: VoiceModel): Boolean = true
    override fun getDownloadedModels(): List<VoiceModel> = models
}
