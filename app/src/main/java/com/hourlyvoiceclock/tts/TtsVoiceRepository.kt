package com.hourlyvoiceclock.tts

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

    fun getVoicesGroupedByLocale(): Map<String, List<VoiceInfo>> {
        return engine.getVoices().groupBy { it.localeDisplayName }
    }

    fun getAllVoices(): List<VoiceInfo> = engine.getVoices()

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

    suspend fun previewVoice(text: String = "The time is 3:45 PM."): Boolean {
        return engine.speak(text)
    }

    fun shutdown() {
        engine.shutdown()
        initialized = false
    }

    fun hasMultipleVoices(): Boolean = engine.getVoices().size > 1
}
