package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.hourlyvoiceclock.announcer.AudioChannelMapping
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineInfo
import com.hourlyvoiceclock.tts.VoiceInfo
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

/**
 * Local TTS engine backed by Sherpa-ONNX's VITS offline TTS, using
 * Piper voice distributions downloaded into the app's filesDir.
 *
 * The espeak-ng-data phoneme directory is bundled in APK assets and
 * loaded via [OfflineTts]'s AssetManager-aware constructor. Per-voice
 * `model.onnx` and `tokens.txt` live in the app's filesDir because
 * they are too large to bundle (60+ MB each) and to allow the user to
 * add voices without an app update.
 */
class LocalTtsEngine(private val context: Context) : TtsEngine {

    private val downloader = OnnxModelDownloader(context)
    private var currentModel: VoiceModel? = null
    private var currentAudioChannel: AudioChannel = AudioChannel.MEDIA
    private var isInitialized = false
    private var tts: OfflineTts? = null

    override suspend fun initialize(enginePackage: String?): Boolean {
        val modelId = enginePackage ?: return false
        val model = VoiceModelRegistry.getVoiceById(modelId) ?: return false

        if (!downloader.isModelDownloaded(model)) {
            Log.w(TAG, "Model ${model.id} not downloaded yet")
            return false
        }

        return try {
            shutdown()
            tts = buildOfflineTts(model)
            currentModel = model
            isInitialized = true
            Log.d(TAG, "Initialized with model: ${model.id}")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize ONNX TTS with model ${model.id}", e)
            shutdown()
            false
        }
    }

    /**
     * Build the Sherpa-ONNX OfflineTts for the given voice. The
     * constructor with an AssetManager resolves `dataDir` against
     * APK assets; `model` and `tokens` are absolute file paths
     * because they live in the app's filesDir (downloaded per voice).
     */
    private fun buildOfflineTts(model: VoiceModel): OfflineTts {
        val modelDir = File(context.filesDir, "local_tts/models/${model.id}")
        val modelFile = File(modelDir, model.onnxFileName)
        val tokensFile = File(modelDir, "tokens.txt")

        require(modelFile.exists()) { "Model file missing: ${modelFile.absolutePath}" }
        require(tokensFile.exists()) { "tokens.txt missing: ${tokensFile.absolutePath}" }

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = modelFile.absolutePath,
            lexicon = "",
            tokens = tokensFile.absolutePath,
            dataDir = ASSET_ESPEAK_DIR,    // resolved by AssetManager
            dictDir = "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f,
        )

        val modelConfig = OfflineTtsModelConfig(
            vits = vitsConfig,
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )

        val config = OfflineTtsConfig(
            model = modelConfig,
            ruleFsts = "",
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f,
        )

        return OfflineTts(context.assets, config)
    }

    override fun isAvailable(): Boolean = isInitialized && tts != null && currentModel != null

    override fun getVoices(): List<VoiceInfo> {
        return downloader.getDownloadedModels().map { model ->
            VoiceInfo(
                name = model.id,
                localeDisplayName = model.language,
                localeTag = model.language,
                quality = 4,
                latency = 2,
                requiresNetwork = false,
                genderLabel = null,
                description = model.displayName,
                isSpecial = true
            )
        }
    }

    override fun setVoice(voiceName: String, localeTag: String): Boolean {
        val model = VoiceModelRegistry.getVoiceById(voiceName) ?: return false
        if (!downloader.isModelDownloaded(model)) return false
        if (currentModel?.id == model.id && tts != null) return true

        return try {
            shutdown()
            tts = buildOfflineTts(model)
            currentModel = model
            isInitialized = true
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to init for voice $voiceName", e)
            shutdown()
            false
        }
    }

    override fun setLanguage(localeTag: String): Boolean {
        val models = downloader.getDownloadedModels()
        val match = models.find { it.language == localeTag } ?: return false
        return setVoice(match.id, localeTag)
    }

    override fun setPitch(pitch: Float) {
        // Piper VITS models don't support real-time pitch adjustment.
    }

    override fun setSpeechRate(rate: Float) {
        // Piper VITS models don't support real-time rate adjustment.
    }

    override fun setAudioChannel(channel: AudioChannel) {
        currentAudioChannel = channel
    }

    override fun speak(text: String, utteranceId: String) {
        speakAsync(text) { /* fire-and-forget */ }
    }

    override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {
        val ttsInstance = tts
        if (ttsInstance == null) {
            onComplete(false)
            return
        }
        val sampleRate = currentModel?.sampleRate ?: 22050

        Thread {
            var success = false
            try {
                val genConfig = GenerationConfig(
                    silenceScale = 0.2f,
                    speed = 1.0f,
                    sid = 0,
                    referenceAudio = null,
                    referenceSampleRate = 0,
                    referenceText = "",
                    numSteps = 0,
                    extra = emptyMap(),
                )
                ttsInstance.generateWithConfigAndCallback(
                    text = text,
                    config = genConfig,
                    callback = { samples ->
                        // Callback runs on the synth thread. Any throw
                        // here aborts generation, so we trap it and
                        // return 0 to stop further chunks.
                        try {
                            playSamples(samples, sampleRate)
                            1
                        } catch (e: Throwable) {
                            Log.e(TAG, "playSamples failed", e)
                            0
                        }
                    },
                )
                success = true
                Log.d(TAG, "Spoke with ${currentModel?.displayName}: $text")
            } catch (e: Throwable) {
                Log.e(TAG, "Async speak failed", e)
            } finally {
                onComplete(success)
            }
        }.start()
    }

    override fun stop() {
        // OfflineTts has no direct stop; the worker thread will exit
        // when generation completes. AudioTrack playback is
        // best-effort interrupted on the next call to shutdown().
    }

    override fun shutdown() {
        try {
            tts?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing OfflineTts", e)
        }
        tts = null
        isInitialized = false
        currentModel = null
    }

    override suspend fun switchEngine(enginePackage: String?): Boolean {
        shutdown()
        return initialize(enginePackage)
    }

    override fun getEngines(): List<TtsEngineInfo> {
        return downloader.getDownloadedModels().map { model ->
            TtsEngineInfo(
                packageName = model.id,
                label = model.displayName,
                isInstalled = true
            )
        }
    }

    override fun getCurrentEnginePackage(): String? = currentModel?.id

    override fun isEspeakNgEngine(): Boolean = false

    fun getInstalledModels(): List<VoiceModel> = downloader.getDownloadedModels()

    fun getModelDownloader(): OnnxModelDownloader = downloader

    private fun playSamples(samples: FloatArray, sampleRate: Int) {
        val spec = AudioChannelMapping.specOf(currentAudioChannel)

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, samples.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(spec.usage)
                    .setContentType(spec.contentType)
                    .build()
            )
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val pcmData = ShortArray(samples.size) { i ->
            val clamped = samples[i].coerceIn(-1f, 1f)
            (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        try {
            track.play()
            track.write(pcmData, 0, pcmData.size)
            val playbackDurationMs = (samples.size.toLong() * 1000) / sampleRate
            Thread.sleep(playbackDurationMs + 50)
        } finally {
            try {
                track.stop()
            } catch (_: IllegalStateException) {
                // already stopped
            }
            track.release()
        }
    }

    companion object {
        private const val TAG = "LocalTtsEngine"

        /**
         * Asset path passed to Sherpa-ONNX as the espeak-ng-data
         * directory. Resolved against the APK's `assets/` via
         * [android.content.res.AssetManager]. The directory is
         * populated by extracting `piper/espeak-ng-data/` from the
         * Piper release tarball at
         * https://github.com/rhasspy/piper/releases/tag/2023.11.14-2 .
         */
        const val ASSET_ESPEAK_DIR = "espeak-ng-data"
    }
}
