package com.hourlyvoiceclock.tts.local

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Prepares a downloaded Piper voice model for use by the native Sherpa-ONNX
 * TTS runtime: validates files, patches ONNX metadata if necessary, and
 * ensures the bundled espeak-ng-data is copied into filesDir.
 *
 * This class concentrates all file-system and asset-management concerns so
 * [LocalTtsEngine] can focus on lifecycle and orchestration.
 */
open class LocalTtsModelLoader(
    private val context: Context,
    private val downloader: OnnxModelDownloader
) {

    data class PreparedModel(
        val modelDir: File,
        val modelFileName: String,
        val espeakDataDir: File
    )

    /**
     * Validates and prepares the model directory. Returns a [PreparedModel]
     * on success or a failure result describing what is missing.
     */
    open fun prepareModel(model: VoiceModel): Result<PreparedModel> {
        if (!downloader.isModelDownloaded(model)) {
            return Result.failure(IllegalStateException("Model ${model.id} not downloaded yet"))
        }

        val modelDir = File(context.filesDir, "local_tts/models/${model.id}")
        val modelFile = File(modelDir, model.onnxFileName)
        val jsonFile = File(modelDir, model.onnxJsonFileName)
        val tokensFile = File(modelDir, "tokens.txt")

        if (!modelFile.exists()) return Result.failure(IllegalStateException("Model file missing: ${modelFile.absolutePath}"))
        if (!jsonFile.exists()) return Result.failure(IllegalStateException("Model JSON missing: ${jsonFile.absolutePath}"))
        if (!tokensFile.exists()) return Result.failure(IllegalStateException("tokens.txt missing: ${tokensFile.absolutePath}"))

        downloader.generateTokensFile(jsonFile, tokensFile).getOrElse { error ->
            return Result.failure(IllegalStateException("Failed to regenerate tokens.txt for ${model.id}: ${error.message}", error))
        }

        val espeakDataDir = prepareEspeakData()
            ?: return Result.failure(IllegalStateException("espeak-ng-data missing"))

        ensurePiperOnnxMetadata(modelFile, jsonFile)

        return Result.success(
            PreparedModel(
                modelDir = modelDir,
                modelFileName = model.onnxFileName,
                espeakDataDir = espeakDataDir
            )
        )
    }

    private fun prepareEspeakData(): File? {
        val espeakDataDir = File(context.filesDir, "local_tts/$ASSET_ESPEAK_DIR")
        val espeakVersionMarker = File(espeakDataDir, ".version-2")
        if (!espeakVersionMarker.exists()) {
            espeakDataDir.deleteRecursively()
            copyAssetsToFiles(ASSET_ESPEAK_DIR, espeakDataDir)
            espeakVersionMarker.createNewFile()
        }
        return if (espeakDataDir.exists()) espeakDataDir else null
    }

    private fun ensurePiperOnnxMetadata(modelFile: File, jsonFile: File) {
        // Sherpa-ONNX's Piper/VITS loader requires all of these metadata keys.
        // The logcat from the failing build showed the native loader accepted
        // voice/sample_rate/n_speakers/comment, then aborted immediately after:
        //   'language' does not exist in the metadata
        // So language is not optional for this AAR path.
        //
        // Do NOT strip or rewrite existing metadata entries. Appending missing
        // metadata_props entries is safe; hand-rebuilding the ONNX protobuf is not.
        val root = JSONObject(jsonFile.readText())
        val sampleRate = root.optJSONObject("audio")
            ?.optInt("sample_rate", 22050)
            ?.takeIf { it > 0 }
            ?.toString()
            ?: "22050"
        val numSpeakers = root.optInt("num_speakers", 1).takeIf { it > 0 }?.toString() ?: "1"
        val espeakVoice = root.optJSONObject("espeak")
            ?.optString("voice")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
            ?: defaultEspeakVoice(root)
        val language = root.optJSONObject("language")
            ?.optString("code")
            ?.takeIf { it.isNotBlank() }
            ?.replace('_', '-')
            ?: defaultLanguageForVoice(espeakVoice)

        val props = listOf(
            "sample_rate" to sampleRate,
            "n_speakers" to numSpeakers,
            "voice" to espeakVoice,
            "language" to language,
            "comment" to "piper",
        ).filterNot { (key, _) -> fileContainsAscii(modelFile, key) }

        if (props.isEmpty()) return

        appendOnnxMetadataProps(modelFile, props)
        Log.d(TAG, "Patched ${modelFile.name} ONNX metadata ${props.joinToString { "${it.first}=${it.second}" }}")
    }

    private fun defaultEspeakVoice(root: JSONObject): String {
        val code = root.optJSONObject("language")
            ?.optString("code")
            ?.lowercase()
            .orEmpty()
        return when {
            code.contains("gb") || code.contains("uk") -> "en-gb"
            else -> "en"
        }
    }

    private fun defaultLanguageForVoice(espeakVoice: String): String {
        return when (espeakVoice.lowercase()) {
            "en-gb", "en-gb-scotland", "en-gb-x-gbclan", "en-gb-x-gbcwmd", "en-gb-x-rp" -> "en-GB"
            "en-us", "en-us-nyc" -> "en-US"
            "en" -> "en-US"
            else -> espeakVoice.replace('_', '-')
        }
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

    companion object {
        private const val TAG = "LocalTtsModelLoader"
        const val ASSET_ESPEAK_DIR = "espeak-ng-data"
    }
}
