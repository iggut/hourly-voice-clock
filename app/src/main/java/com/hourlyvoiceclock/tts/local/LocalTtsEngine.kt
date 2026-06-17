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
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Local TTS engine backed by Sherpa-ONNX's VITS offline TTS using Piper voice
 * distributions downloaded into the app's filesDir.
 *
 * Uses the bundled com.k2fsa.sherpa.onnx.OfflineTts API directly. The model
 * directory must contain the .onnx, the .onnx.json (config + sample rate),
 * and tokens.txt; the dataDir must point at the bundled espeak-ng-data.
 */
class LocalTtsEngine(private val context: Context) : TtsEngine {

    private val downloader = OnnxModelDownloader(context)
    private var currentModel: VoiceModel? = null
    private var currentAudioChannel: AudioChannel = AudioChannel.MEDIA
    private var isInitialized = false
    @Volatile private var isSynthesizing = false
    @Volatile private var cancelRequested = false
    private var tts: OfflineTts? = null
    private var sampleRate: Int = 22050

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
            sampleRate = tts?.sampleRate() ?: 22050
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

    private fun buildTts(model: VoiceModel): OfflineTts {
        val modelDir = File(context.filesDir, "local_tts/models/${model.id}")
        val modelFile = File(modelDir, model.onnxFileName)
        val jsonFile = File(modelDir, model.onnxJsonFileName)
        val tokensFile = File(modelDir, "tokens.txt")

        require(modelFile.exists()) { "Model file missing: ${modelFile.absolutePath}" }
        require(jsonFile.exists()) { "Model JSON missing: ${jsonFile.absolutePath}" }
        require(tokensFile.exists()) { "tokens.txt missing: ${tokensFile.absolutePath}" }

        downloader.generateTokensFile(jsonFile, tokensFile).getOrElse { error ->
            throw IllegalStateException("Failed to regenerate tokens.txt for ${model.id}: ${error.message}", error)
        }

        val espeakDataDir = File(context.filesDir, "local_tts/$ASSET_ESPEAK_DIR")
        val espeakVersionMarker = File(espeakDataDir, ".version-2")
        if (!espeakVersionMarker.exists()) {
            espeakDataDir.deleteRecursively()
            copyAssetsToFiles(ASSET_ESPEAK_DIR, espeakDataDir)
            espeakVersionMarker.createNewFile()
        }
        require(espeakDataDir.exists()) { "espeak-ng-data missing: ${espeakDataDir.absolutePath}" }

        ensurePiperOnnxMetadata(modelFile, jsonFile)

        // Build the VITS config from the sanitized AAR's TtsKt.getOfflineTtsConfig
        // factory. The factory constructs `model = "$modelDir/$modelName"`
        // and passes that as the absolute path to ReadFile, so modelName
        // MUST include the `.onnx` extension. dataDir is the espeak-ng-data
        // dir; tokens.txt is auto-resolved as `$modelDir/tokens.txt` by
        // the factory. The .onnx.json sibling is read directly by the
        // native VITS model loader.
        val config: OfflineTtsConfig = getOfflineTtsConfig(
            modelDir = modelDir.absolutePath,
            modelName = model.onnxFileName,        // includes ".onnx"
            acousticModelName = "",      // Matcha (unused for VITS)
            vocoder = "",                 // Matcha (unused for VITS)
            voices = "",                  // Kokoro
            lexicon = "",                 // no lexicon for Piper VITS
            dataDir = espeakDataDir.absolutePath,
            dictDir = "",                 // Coqui
            ruleFsts = "",                // Coqui
            ruleFars = "",                // Coqui
            numThreads = 1,               // keep ORT single-threaded on mobile
            isKitten = false,             // not a Kitten model
            isSupertonic = false,         // not a Supertonic model
            durationPredictor = "",       // Supertonic
            textEncoder = "",             // Supertonic
            vectorEstimator = "",         // Supertonic
            supertonicVocoder = "",       // Supertonic
            ttsJson = "",                 // Kitten
            unicodeIndexer = "",          // Kitten
            voiceStyle = "",              // voice-style
        )

        // Pass null for the AssetManager so the native side reads the
        // model from the filesystem (SD card / filesDir) instead of
        // trying to open it as an APK asset. See:
        //   https://github.com/k2-fsa/sherpa-onnx/issues/2562
        return OfflineTts(null, config)
    }

    private fun ensurePiperOnnxMetadata(modelFile: File, jsonFile: File) {
        // Migration: older patches used `language=en_US` (wrong key, and
        // an underscore that eSpeak-ng rejects). Strip any such legacy
        // entries first so the new patch below produces a clean file.
        stripOnnxMetadataKey(modelFile, "language")
        stripOnnxMetadataKey(modelFile, "comment")

        // Sherpa-ONNX v1.13.x reads VITS metadata (sample_rate, n_speakers,
        // voice, comment, etc.) from ONNX custom metadata. Piper ships its
        // config in the sibling .onnx.json. Patch them in-place:
        //   sample_rate  <- json.audio.sample_rate
        //   n_speakers   <- json.num_speakers  (default 1)
        //   voice        <- json.espeak.voice  (e.g. "en-us")
        //   comment      <- "piper"  (so meta_data_.is_piper = true)
        val json = jsonFile.readText()
        val sampleRate = Regex(""""sample_rate"\s*:\s*(\d+)""")
            .find(json)
            ?.groupValues
            ?.getOrNull(1)
            ?: "22050"
        val numSpeakers = Regex(""""num_speakers"\s*:\s*(\d+)""")
            .find(json)
            ?.groupValues
            ?.getOrNull(1)
            ?: "1"
        // espeak.voice is required for VITS/Piper — it is the name passed
        // to espeak_SetVoiceByName. Without it the native code calls
        // espeak_SetVoiceByName("") which throws std::runtime_error
        // ("Failed to set eSpeak-ng voice"), escaping the JNI boundary
        // and aborting the process. Fall back to en-us (the rhasspy
        // piper-voices default) if the JSON omits it.
        val espeakVoice = Regex(""""voice"\s*:\s*"([^"]+)"""")
            .find(json)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
            ?: "en-us"

        val props = listOf(
            "sample_rate" to sampleRate,
            "n_speakers" to numSpeakers,
            "voice" to espeakVoice,
            "comment" to "piper",
        ).filterNot { (key, _) -> fileContainsAscii(modelFile, key) }

        if (props.isEmpty()) return

        appendOnnxMetadataProps(modelFile, props)
        Log.d(TAG, "Patched ${modelFile.name} ONNX metadata ${props.joinToString { "${it.first}=${it.second}" }}")
    }

    /**
     * Strip all ONNX metadata_props entries whose key field equals [key].
     * This rebuilds the file, so the returned byte length may be smaller.
     * Used to clean up legacy patches that wrote wrong-keyed entries.
     */
    private fun stripOnnxMetadataKey(file: File, key: String) {
        val bytes = file.readBytes()
        // Walk the protobuf, find every ModelProto.metadata_props entry,
        // drop entries whose StringStringEntryProto.key == [key].
        // metadata_props is field 14, length-delimited.
        val out = ArrayList<Byte>(bytes.size)
        var i = 0
        var modified = false
        val needle = key.toByteArray(StandardCharsets.UTF_8)

        while (i < bytes.size) {
            val tag = bytes[i].toInt() and 0xFF
            val wireType = tag and 0x07
            val fieldNum = tag ushr 3

            if (fieldNum == 14 && wireType == 2) {
                // Read length varint
                i++
                var len = 0
                var shift = 0
                while (i < bytes.size && bytes[i].toInt() and 0x80 != 0) {
                    len = len or ((bytes[i].toInt() and 0x7F) shl shift)
                    shift += 7
                    i++
                }
                if (i < bytes.size) {
                    len = len or (bytes[i].toInt() shl shift)
                    i++
                }
                val entryEnd = i + len
                if (entryEnd > bytes.size) {
                    // Corrupt — abort stripping, keep file intact.
                    return
                }
                val entryBytes = bytes.copyOfRange(i, entryEnd)
                val entryKey = readStringStringEntryKey(entryBytes)
                if (entryKey != null && entryKey.contentEquals(needle)) {
                    // Drop this entry entirely.
                    modified = true
                    i = entryEnd
                    continue
                }
                // Keep the original tag + length + entry bytes.
                // Emit tag byte
                out.add(tag.toByte())
                // Re-emit the length varint we just decoded
                var lv = len
                val temp = ArrayList<Byte>()
                while (lv >= 0x80) {
                    temp.add(((lv and 0x7F) or 0x80).toByte())
                    lv = lv ushr 7
                }
                temp.add(lv.toByte())
                out.addAll(temp)
                for (b in entryBytes) out.add(b)
                i = entryEnd
                continue
            }

            // Pass through any other field unchanged.
            out.add(bytes[i])
            i++
            if (wireType == 2) {
                // length-delimited: re-emit the length and payload
                var len = 0
                var shift = 0
                while (i < bytes.size && bytes[i].toInt() and 0x80 != 0) {
                    len = len or ((bytes[i].toInt() and 0x7F) shl shift)
                    shift += 7
                    i++
                }
                if (i < bytes.size) {
                    len = len or (bytes[i].toInt() shl shift)
                    i++
                }
                var lv = len
                val temp = ArrayList<Byte>()
                while (lv >= 0x80) {
                    temp.add(((lv and 0x7F) or 0x80).toByte())
                    lv = lv ushr 7
                }
                temp.add(lv.toByte())
                out.addAll(temp)
                for (k in 0 until len) {
                    if (i + k < bytes.size) out.add(bytes[i + k])
                }
                i += len
            } else if (wireType == 0 || wireType == 1 || wireType == 5) {
                // varint: read bytes with high bit set + final byte
                // 32-bit (wireType=5) / 64-bit (wireType=1) need to consume
                // a fixed number of bytes; we just consume until high bit clear
                // for varint, and 4 / 8 bytes for fixed.
                if (wireType == 0) {
                    while (i < bytes.size && bytes[i].toInt() and 0x80 != 0) {
                        out.add(bytes[i])
                        i++
                    }
                    if (i < bytes.size) {
                        out.add(bytes[i])
                        i++
                    }
                } else {
                    val n = if (wireType == 5) 4 else 8
                    for (k in 0 until n) {
                        if (i + k < bytes.size) out.add(bytes[i + k])
                    }
                    i += n
                }
            }
            // wireType 3 / 4 are deprecated
        }

        if (modified) {
            file.writeBytes(out.toByteArray())
            Log.d(TAG, "Stripped legacy metadata key '$key' from ${file.name}")
        }
    }

    private fun readStringStringEntryKey(entry: ByteArray): ByteArray? {
        // StringStringEntryProto: field 1 (key) wire 2, then field 2 (value) wire 2.
        var i = 0
        // tag
        if (i >= entry.size) return null
        val tag = entry[i].toInt() and 0xFF
        val wire = tag and 0x07
        val field = tag ushr 3
        if (field != 1 || wire != 2) return null
        i++
        var len = 0
        var shift = 0
        while (i < entry.size && entry[i].toInt() and 0x80 != 0) {
            len = len or ((entry[i].toInt() and 0x7F) shl shift)
            shift += 7
            i++
        }
        if (i < entry.size) {
            len = len or (entry[i].toInt() shl shift)
            i++
        }
        if (i + len > entry.size) return null
        return entry.copyOfRange(i, i + len)
    }

    private fun fileContainsAscii(file: File, needle: String): Boolean {
        val needleBytes = needle.toByteArray(StandardCharsets.UTF_8)
        val buffer = ByteArray(64 * 1024)
        var matched = 0
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) return false
                for (i in 0 until read) {
                    matched = if (buffer[i] == needleBytes[matched]) {
                        matched + 1
                    } else if (buffer[i] == needleBytes[0]) {
                        1
                    } else {
                        0
                    }
                    if (matched == needleBytes.size) return true
                }
            }
        }
    }

    private fun appendOnnxMetadataProps(file: File, props: List<Pair<String, String>>) {
        FileOutputStream(file, true).use { output ->
            props.forEach { (key, value) ->
                val entry = ByteArrayOutput()
                entry.writeVarint((1 shl 3) or 2) // StringStringEntryProto.key
                entry.writeLengthDelimited(key.toByteArray(StandardCharsets.UTF_8))
                entry.writeVarint((2 shl 3) or 2) // StringStringEntryProto.value
                entry.writeLengthDelimited(value.toByteArray(StandardCharsets.UTF_8))

                output.writeVarint((14 shl 3) or 2) // ModelProto.metadata_props
                output.writeVarint(entry.size)
                output.write(entry.toByteArray())
            }
        }
    }

    private class ByteArrayOutput {
        private val bytes = ArrayList<Byte>()
        val size: Int get() = bytes.size

        fun writeVarint(value: Int) {
            var v = value
            while (v >= 0x80) {
                bytes.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            bytes.add(v.toByte())
        }

        fun writeLengthDelimited(data: ByteArray) {
            writeVarint(data.size)
            data.forEach { bytes.add(it) }
        }

        fun toByteArray(): ByteArray = ByteArray(bytes.size) { i -> bytes[i] }
    }

    private fun FileOutputStream.writeVarint(value: Int) {
        var v = value
        while (v >= 0x80) {
            write(((v and 0x7F) or 0x80))
            v = v ushr 7
        }
        write(v)
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
            sampleRate = tts?.sampleRate() ?: 22050
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
                val audio: GeneratedAudio? = ttsInstance.generate(text, 0, 1.0f)
                val samples = audio?.samples
                if (samples != null && samples.isNotEmpty() && !cancelRequested) {
                    val sr = audio.sampleRate.takeIf { it > 0 } ?: sampleRate
                    playSamples(samples, sr)
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
        const val ASSET_ESPEAK_DIR = "espeak-ng-data"
    }
}
