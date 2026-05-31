package com.hourlyvoiceclock.tts

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TtsVoiceRepository(private val engine: TtsEngine) {

    private var initialized = false

    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        if (initialized) return@withContext true
        val result = engine.initialize()
        initialized = result
        result
    }

    fun isAvailable(): Boolean = engine.isAvailable()

    fun getSpecialVoices(): List<VoiceInfo> {
        return filteredVoices().filter { it.isSpecial }
    }

    fun getNormalVoicesGroupedByLocale(): Map<String, List<VoiceInfo>> {
        return filteredVoices().filter { !it.isSpecial }.groupBy { it.localeDisplayName }
    }

    fun getAllVoices(): List<VoiceInfo> = filteredVoices()

    private fun filteredVoices(): List<VoiceInfo> {
        return engine.getVoices().filter { voice ->
            voice.localeTag.isSupportedAnnouncementLocaleTag()
        }
    }

    private fun String.isSupportedAnnouncementLocaleTag(): Boolean {
        return startsWith("en", ignoreCase = true) ||
                startsWith("fr", ignoreCase = true) ||
                startsWith("eng", ignoreCase = true) ||
                startsWith("fra", ignoreCase = true) ||
                startsWith("fre", ignoreCase = true)
    }

    fun selectVoice(voiceName: String, localeTag: String): Boolean {
        return engine.setVoice(voiceName, localeTag)
    }

    fun selectLanguage(localeTag: String): Boolean {
        return engine.setLanguage(localeTag)
    }

    fun setPitch(pitch: Float) {
        engine.setPitch(pitch)
    }

    fun setSpeechRate(rate: Float) {
        engine.setSpeechRate(rate)
    }

    fun setAudioChannel(channel: com.hourlyvoiceclock.data.AudioChannel) {
        engine.setAudioChannel(channel)
    }

    fun previewVoice(text: String = "The time is 3:45 PM.") {
        if (!engine.isAvailable()) {
            Log.w("TtsVoiceRepository", "previewVoice called but engine not available")
            return
        }
        (engine as? AndroidTtsEngine)?.speakAsync(text) { success ->
            Log.d("TtsVoiceRepository", "previewVoice completed: success=$success")
        } ?: engine.speak(text, "preview_${System.currentTimeMillis()}")
    }

    fun speak(text: String) {
        if (!engine.isAvailable()) {
            Log.w("TtsVoiceRepository", "speak called but engine not available")
            return
        }
        (engine as? AndroidTtsEngine)?.speakAsync(text) { success ->
            Log.d("TtsVoiceRepository", "speak completed: success=$success")
        } ?: engine.speak(text, "speak_${System.currentTimeMillis()}")
    }

    fun hasMultipleVoices(): Boolean = engine.getVoices().size > 1

    suspend fun switchEngine(enginePackage: String?): Boolean = withContext(Dispatchers.Main) {
        val result = engine.switchEngine(enginePackage)
        initialized = result
        result
    }

    fun getEngines(): List<TtsEngineInfo> {
        return engine.getEngines()
    }
}
