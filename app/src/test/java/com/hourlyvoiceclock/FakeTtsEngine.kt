package com.hourlyvoiceclock

import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.VoiceInfo

class FakeTtsEngine : TtsEngine {

    var initialized = false
    var lastSpokenText: String? = null
    var lastPitch = 1.0f
    var lastSpeechRate = 1.0f
    var lastVoiceName: String? = null
    var lastLocaleTag: String? = null
    private val fakeVoices = mutableListOf<VoiceInfo>()

    fun addFakeVoice(voice: VoiceInfo) {
        fakeVoices.add(voice)
    }

    override suspend fun initialize(): Boolean {
        initialized = true
        return true
    }

    override fun getVoices(): List<VoiceInfo> = fakeVoices.toList()

    override fun setVoice(voiceName: String, localeTag: String): Boolean {
        lastVoiceName = voiceName
        lastLocaleTag = localeTag
        return true
    }

    override fun setLanguage(localeTag: String): Boolean {
        lastLocaleTag = localeTag
        return true
    }

    override fun setPitch(pitch: Float) {
        lastPitch = pitch
    }

    override fun setSpeechRate(rate: Float) {
        lastSpeechRate = rate
    }

    override suspend fun speak(text: String): Boolean {
        lastSpokenText = text
        return true
    }

    override fun shutdown() {
        initialized = false
    }
}
