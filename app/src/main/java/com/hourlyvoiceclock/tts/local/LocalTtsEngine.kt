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

        shutdown()

        val modelDir = File(context.filesDir, "local_tts/models/${model.id}")
        val modelFile = File(modelDir, model.onnxFileName)
        val tokensFile = File(modelDir, "tokens.txt")
        val espeakDataDir = File(modelDir, "espeak-ng-data")

        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
            return false
        }

        // Piper models need tokens.txt and espeak-ng-data
        // For now, check if tokens exist; if not, model may be incomplete
        if (!tokensFile.exists()) {
            Log.w(TAG, "tokens.txt not found at ${tokensFile.absolutePath}, attempting with model only")
        }

        try {
            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelFile.absolutePath,
                tokens = if (tokensFile.exists()) tokensFile.absolutePath else "",
                dataDir = if (espeakDataDir.exists()) espeakDataDir.absolutePath else "",
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = 2,
                debug = false,
            )

            val config = OfflineTtsConfig(model = modelConfig)
            tts = OfflineTts(config = config)

            currentModel = model
            isInitialized = true
            Log.d(TAG, "Initialized with model: ${model.id}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX TTS with model ${model.id}", e)
            return false
        }
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
        if (currentModel?.id != model.id) {
            // Initialize synchronously on the calling thread
            shutdown()
            val modelDir = File(context.filesDir, "local_tts/models/${model.id}")
            val modelFile = File(modelDir, model.onnxFileName)
            val tokensFile = File(modelDir, "tokens.txt")
            val espeakDataDir = File(modelDir, "espeak-ng-data")

            if (!modelFile.exists()) return false

            try {
                val vitsConfig = OfflineTtsVitsModelConfig(
                    model = modelFile.absolutePath,
                    tokens = if (tokensFile.exists()) tokensFile.absolutePath else "",
                    dataDir = if (espeakDataDir.exists()) espeakDataDir.absolutePath else "",
                )
                val modelConfig = OfflineTtsModelConfig(vits = vitsConfig, numThreads = 2, debug = false)
                tts = OfflineTts(config = OfflineTtsConfig(model = modelConfig))
                currentModel = model
                isInitialized = true
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init for voice $voiceName", e)
                return false
            }
        }
        return true
    }

    override fun setLanguage(localeTag: String): Boolean {
        val models = downloader.getDownloadedModels()
        val match = models.find { it.language == localeTag } ?: return false
        return setVoice(match.id, localeTag)
    }

    override fun setPitch(pitch: Float) {
        // Piper VITS models don't support real-time pitch adjustment
    }

    override fun setSpeechRate(rate: Float) {
        // Piper VITS models don't support real-time rate adjustment
    }

    override fun setAudioChannel(channel: AudioChannel) {
        currentAudioChannel = channel
    }

    override fun speak(text: String, utteranceId: String) {
        val ttsInstance = tts ?: run {
            Log.e(TAG, "speak() called but TTS not initialized")
            return
        }

        val sampleRate = currentModel?.sampleRate ?: 22050

        Thread {
            try {
                val genConfig = GenerationConfig(silenceScale = 0.2f)
                val audio = ttsInstance.generateWithConfigAndCallback(
                    text = text,
                    config = genConfig,
                    callback = { samples ->
                        playSamples(samples, sampleRate)
                        1
                    }
                )
                Log.d(TAG, "Spoke with ${currentModel?.displayName}: $text")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to speak with local engine", e)
            }
        }.start()
    }

    override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {
        val ttsInstance = tts
        if (ttsInstance == null) {
            onComplete(false)
            return
        }

        val sampleRate = currentModel?.sampleRate ?: 22050

        Thread {
            try {
                val genConfig = GenerationConfig(silenceScale = 0.2f)
                val audio = ttsInstance.generateWithConfigAndCallback(
                    text = text,
                    config = genConfig,
                    callback = { samples ->
                        playSamples(samples, sampleRate)
                        1
                    }
                )
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Async speak failed", e)
                onComplete(false)
            }
        }.start()
    }

    override fun stop() {
        // There's no direct stop method on OfflineTts, but we can release and re-init
    }

    override fun shutdown() {
        tts?.release()
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

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(spec.usage)
                    .setContentType(spec.contentType)
                    .build()
            )
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val pcmData = ShortArray(samples.size) { i ->
            val clamped = samples[i].coerceIn(-1f, 1f)
            (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        track.play()
        track.write(pcmData, 0, pcmData.size)

        // Wait for playback to finish
        val playbackDurationMs = (samples.size.toLong() * 1000) / sampleRate
        Thread.sleep(playbackDurationMs + 50)

        track.stop()
        track.release()
    }

    companion object {
        private const val TAG = "LocalTtsEngine"
    }
}
