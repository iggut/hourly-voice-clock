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
import java.io.File

/**
 * Local TTS engine backed by Sherpa-ONNX's VITS offline TTS via the C API,
 * using Piper voice distributions downloaded into the app's filesDir.
 *
 * The native Java JNI layer in the Sherpa-ONNX AAR crashes on some devices
 * (Android 16 / Samsung) due to a JNI field-ID mismatch in
 * OfflineTts.newFromAsset. We bypass it entirely by calling the stable C
 * API through a tiny custom JNI bridge (libnative-tts-bridge.so).
 */
class LocalTtsEngine(private val context: Context) : TtsEngine {

    private val downloader = OnnxModelDownloader(context)
    private var currentModel: VoiceModel? = null
    private var currentAudioChannel: AudioChannel = AudioChannel.MEDIA
    private var isInitialized = false
    @Volatile private var isSynthesizing = false
    @Volatile private var cancelRequested = false
    private var tts: NativeTtsBridge? = null

    override suspend fun initialize(enginePackage: String?): Boolean {
        val modelId = enginePackage ?: return false
        val model = VoiceModelRegistry.getVoiceById(modelId) ?: return false

        if (!downloader.isModelDownloaded(model)) {
            Log.w(TAG, "Model ${model.id} not downloaded yet")
            return false
        }

        synchronized(this) {
            if (isInitialized && currentModel?.id == model.id && tts != null) {
                return true
            }
            if (isSynthesizing) {
                Log.w(TAG, "Refusing to reinitialize while synthesis is active for ${currentModel?.id}")
                return false
            }
        }

        return try {
            shutdown()
            tts = buildTts(model)
            currentModel = model
            isInitialized = true
            cancelRequested = false
            Log.d(TAG, "Initialized with model: ${model.id}")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize ONNX TTS with model ${model.id}", e)
            shutdown()
            false
        }
    }

    private fun buildTts(model: VoiceModel): NativeTtsBridge {
        val modelDir = File(context.filesDir, "local_tts/models/${model.id}")
        val modelFile = File(modelDir, model.onnxFileName)
        val tokensFile = File(modelDir, "tokens.txt")

        require(modelFile.exists()) { "Model file missing: ${modelFile.absolutePath}" }
        require(tokensFile.exists()) { "tokens.txt missing: ${tokensFile.absolutePath}" }

        val espeakDataDir = File(context.filesDir, "local_tts/$ASSET_ESPEAK_DIR")
        if (!espeakDataDir.exists()) {
            copyAssetsToFiles(ASSET_ESPEAK_DIR, espeakDataDir)
        }

        val bridge = NativeTtsBridge.create(
            modelFile.absolutePath,
            tokensFile.absolutePath,
            espeakDataDir.absolutePath
        )
        if (bridge == null) {
            throw IllegalStateException("Failed to create NativeTtsBridge (symbol resolution or native initialization failed)")
        }

        return bridge
    }

    private fun copyAssetsToFiles(assetPath: String, destDir: File) {
        destDir.mkdirs()
        val assetList = context.assets.list(assetPath) ?: return
        for (name in assetList) {
            val assetChild = "$assetPath/$name"
            val destChild = File(destDir, name)
            val children = context.assets.list(assetChild)
            if (children.isNullOrEmpty()) {
                context.assets.open(assetChild).use { input ->
                    destChild.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                copyAssetsToFiles(assetChild, destChild)
            }
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
        synchronized(this) {
            if (isSynthesizing) {
                Log.w(TAG, "Refusing to switch voices while synthesis is active")
                return false
            }
            if (currentModel?.id == model.id && tts != null) return true
        }

        return try {
            shutdown()
            tts = buildTts(model)
            currentModel = model
            isInitialized = true
            cancelRequested = false
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
        synchronized(this) {
            if (isSynthesizing) {
                Log.w(TAG, "speakAsync rejected because synthesis is already active")
                onComplete(false)
                return
            }
            isSynthesizing = true
            cancelRequested = false
        }

        Thread {
            var success = false
            try {
                val samples = ttsInstance.generate(text, sid = 0, speed = 1.0f)
                if (samples != null && !cancelRequested) {
                    playSamples(samples, ttsInstance.sampleRate)
                    success = true
                }
                Log.d(TAG, "Spoke with ${currentModel?.displayName}: $text")
            } catch (e: Throwable) {
                Log.e(TAG, "Async speak failed", e)
            } finally {
                synchronized(this@LocalTtsEngine) {
                    isSynthesizing = false
                    cancelRequested = false
                }
                onComplete(success)
            }
        }.start()
    }

    override fun stop() {
        cancelRequested = true
    }

    override fun shutdown() {
        cancelRequested = true
        synchronized(this) {
            if (isSynthesizing) {
                Log.w(TAG, "Deferring shutdown while synthesis is active")
                return
            }
        }
        try {
            tts?.destroy()
        } catch (e: Throwable) {
            Log.w(TAG, "Error destroying NativeTtsBridge", e)
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
        const val ASSET_ESPEAK_DIR = "espeak-ng-data"
    }
}
