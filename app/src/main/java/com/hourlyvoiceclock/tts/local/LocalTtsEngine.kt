package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineInfo
import com.hourlyvoiceclock.tts.VoiceInfo
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Local TTS engine backed by Sherpa-ONNX's VITS offline TTS using Piper voice
 * distributions downloaded into the app's filesDir.
 *
 * This class is now an orchestrator over three focused modules:
 * - [LocalTtsModelLoader] prepares model files and espeak-ng-data.
 * - [LocalTtsSynthesizer] wraps the native [com.k2fsa.sherpa.onnx.OfflineTts].
 * - [LocalTtsAudioPlayer] streams generated PCM samples.
 */
class LocalTtsEngine(
    private val modelLoader: LocalTtsModelLoader,
    private val synthesizerFactory: (com.k2fsa.sherpa.onnx.OfflineTtsConfig) -> LocalTtsSynthesizer,
    private val audioPlayer: LocalTtsAudioPlayer,
    private val downloader: OnnxModelDownloader,
    private val modelLookup: (String) -> VoiceModel? = { VoiceModelRegistry.getVoiceById(it) },
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) : TtsEngine {

    private var currentModel: VoiceModel? = null
    private var currentAudioChannel: AudioChannel = AudioChannel.MEDIA
    private var isInitialized = false
    @Volatile private var isSynthesizing = false
    @Volatile private var cancelRequested = false
    private var synthesizer: LocalTtsSynthesizer? = null
    private var sampleRate: Int = 22050

    /**
     * Convenience constructor that wires the production implementations.
     */
    constructor(context: Context) : this(
        modelLoader = LocalTtsModelLoader(context, OnnxModelDownloader(context)),
        synthesizerFactory = { config -> OfflineTtsSynthesizer(com.k2fsa.sherpa.onnx.OfflineTts(null, config)) },
        audioPlayer = AudioTrackPlayer(),
        downloader = OnnxModelDownloader(context),
        coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    )

    override suspend fun initialize(enginePackage: String?): Boolean {
        val modelId = enginePackage ?: return false
        val model = modelLookup(modelId) ?: return false

        synchronized(this) {
            if (isInitialized && currentModel?.id == model.id && synthesizer != null) {
                return true
            }
            if (isSynthesizing) {
                Log.w(TAG, "Refusing to reinitialize while synthesis is active for ${currentModel?.id}")
                return false
            }
        }

        return try {
            shutdown()
            synthesizer = buildSynthesizer(model)
            sampleRate = synthesizer?.sampleRate ?: 22050
            currentModel = model
            isInitialized = true
            cancelRequested = false
            Log.d(TAG, "Initialized with model: ${model.id} (sample rate: $sampleRate)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize ONNX TTS with model ${model.id}", e)
            shutdown()
            false
        }
    }

    private fun buildSynthesizer(model: VoiceModel): LocalTtsSynthesizer {
        val prepared = modelLoader.prepareModel(model).getOrElse { error ->
            throw IllegalStateException("Failed to prepare model ${model.id}: ${error.message}", error)
        }

        val config = getOfflineTtsConfig(
            modelDir = prepared.modelDir.absolutePath,
            modelName = prepared.modelFileName,
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = "",
            dataDir = prepared.espeakDataDir.absolutePath,
            dictDir = "",
            ruleFsts = "",
            ruleFars = "",
            numThreads = 1,
            isKitten = false,
            isSupertonic = false,
            durationPredictor = "",
            textEncoder = "",
            vectorEstimator = "",
            supertonicVocoder = "",
            ttsJson = "",
            unicodeIndexer = "",
            voiceStyle = "",
        )

        // Pass null for the AssetManager so the native side reads the model from
        // the filesystem instead of trying to open it as an APK asset.
        return synthesizerFactory(config)
    }

    override fun isAvailable(): Boolean = isInitialized && synthesizer != null && currentModel != null

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
        val model = modelLookup(voiceName) ?: return false
        if (!downloader.isModelDownloaded(model)) return false
        synchronized(this) {
            if (isSynthesizing) {
                Log.w(TAG, "Refusing to switch voices while synthesis is active")
                return false
            }
            if (currentModel?.id == model.id && synthesizer != null) return true
        }

        return try {
            shutdown()
            synthesizer = buildSynthesizer(model)
            sampleRate = synthesizer?.sampleRate ?: 22050
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
        val synth = synthesizer
        if (synth == null) {
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

        coroutineScope.launch {
            var success = false
            try {
                val audio = synth.generate(text, 1.0f)
                val samples = audio?.samples
                if (samples != null && samples.isNotEmpty() && !cancelRequested) {
                    val sr = audio.sampleRate.takeIf { it > 0 } ?: sampleRate
                    audioPlayer.play(samples, sr, currentAudioChannel)
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
        }
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
            synthesizer?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing synthesizer", e)
        }
        coroutineScope.coroutineContext.cancelChildren()
        synthesizer = null
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

    companion object {
        private const val TAG = "LocalTtsEngine"
    }
}
