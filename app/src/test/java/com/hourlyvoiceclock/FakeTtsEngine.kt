package com.hourlyvoiceclock

import com.hourlyvoiceclock.tts.TtsEngine
import com.hourlyvoiceclock.tts.TtsEngineInfo
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.VoiceProfile

class FakeTtsEngine : TtsEngine {

    var initialized = false
    var initializedWithPackage: String? = null
    var lastSpokenText: String? = null
    var lastUtteranceId: String? = null
    var lastPitch = 1.0f
    var lastSpeechRate = 1.0f
    var lastVoiceName: String? = null
    var lastLocaleTag: String? = null
    private val fakeVoices = mutableListOf<VoiceInfo>()

    fun addFakeVoice(voice: VoiceInfo) {
        fakeVoices.add(voice)
    }

    override suspend fun initialize(enginePackage: String?): Boolean {
        initialized = true
        initializedWithPackage = enginePackage
        return true
    }

    override fun isAvailable(): Boolean = initialized

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

    override fun setAudioChannel(channel: com.hourlyvoiceclock.data.AudioChannel) {}

    override fun configure(profile: VoiceProfile): Boolean {
        lastVoiceName = profile.voiceName
        lastLocaleTag = profile.localeTag
        lastPitch = profile.pitch
        lastSpeechRate = profile.speechRate
        return profile.voiceName?.isNotBlank() == true
            || profile.localeTag?.isNotBlank() == true
            || profile.localModelId?.isNotBlank() == true
    }

    override fun speak(text: String, utteranceId: String) {
        lastSpokenText = text
        lastUtteranceId = utteranceId
    }

    override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {
        lastSpokenText = text
        onComplete(true)
    }

    override fun stop() {}

    override fun shutdown() {
        initialized = false
    }

    override suspend fun switchEngine(enginePackage: String?): Boolean {
        lastVoiceName = null
        lastLocaleTag = null
        return true
    }

    override fun getEngines(): List<TtsEngineInfo> {
        return listOf(
            TtsEngineInfo("com.google.android.tts", "Speech Services by Google", true),
            TtsEngineInfo("com.redzoc.ramees.tts.espeak", "eSpeak NG", false),
            TtsEngineInfo("com.github.olga_yakovleva.rhvoice.android", "RHVoice", false)
        )
    }

    override fun getCurrentEnginePackage(): String? = null

    override fun isEspeakNgEngine(): Boolean = false
}
