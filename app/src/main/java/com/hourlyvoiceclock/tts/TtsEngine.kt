package com.hourlyvoiceclock.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.hourlyvoiceclock.data.AudioChannel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

interface TtsEngine {
    suspend fun initialize(): Boolean
    fun isAvailable(): Boolean
    fun getVoices(): List<VoiceInfo>
    fun setVoice(voiceName: String, localeTag: String): Boolean
    fun setLanguage(localeTag: String): Boolean
    fun setPitch(pitch: Float)
    fun setSpeechRate(rate: Float)
    fun setAudioChannel(channel: AudioChannel)
    fun speak(text: String, utteranceId: String)
    fun stop()
    fun shutdown()
}

class AndroidTtsEngine(context: Context) : TtsEngine {

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext
    private var voices: List<VoiceInfo> = emptyList()
    private var initOk = false
    private val utteranceCounter = AtomicInteger(0)
    private val pendingUtterances = mutableMapOf<String, (Boolean) -> Unit>()

    override suspend fun initialize(): Boolean {
        if (initOk) return true
        val success = suspendCancellableCoroutine { continuation ->
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val instance = tts ?: run {
                        continuation.resume(false)
                        return@TextToSpeech
                    }
                    voices = queryVoices(instance)
                    setupProgressListener(instance)
                    setupAudioAttributes(instance)
                    initOk = true
                    continuation.resume(true)
                } else {
                    Log.e("TtsEngine", "TTS init failed with status=$status")
                    continuation.resume(false)
                }
            }
        }
        return success
    }

    override fun isAvailable(): Boolean = initOk && tts != null

    override fun getVoices(): List<VoiceInfo> = voices

    override fun setVoice(voiceName: String, localeTag: String): Boolean {
        if (voiceName.isBlank() || localeTag.isBlank()) return false
        val ttsInstance = tts ?: return false
        val voice = ttsInstance.voices?.find {
            it.name == voiceName && it.locale.toLanguageTag() == localeTag
        } ?: return false
        val result = ttsInstance.setVoice(voice)
        Log.d("TtsEngine", "setVoice(${voice.name}) = $result")
        return result == TextToSpeech.SUCCESS
    }

    override fun setLanguage(localeTag: String): Boolean {
        if (localeTag.isBlank()) return false
        val ttsInstance = tts ?: return false
        val locale = Locale.forLanguageTag(localeTag)
        val result = ttsInstance.setLanguage(locale)
        Log.d("TtsEngine", "setLanguage($localeTag) = $result")
        return result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || result == TextToSpeech.LANG_AVAILABLE
                || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    override fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.1f, 2.0f))
    }

    override fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.1f, 2.0f))
    }

    override fun setAudioChannel(channel: AudioChannel) {
        val ttsInstance = tts ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val (usage, contentType) = when (channel) {
                AudioChannel.MEDIA ->
                    AudioAttributes.USAGE_MEDIA to AudioAttributes.CONTENT_TYPE_SPEECH
                AudioChannel.NOTIFICATION ->
                    AudioAttributes.USAGE_NOTIFICATION to AudioAttributes.CONTENT_TYPE_SPEECH
                AudioChannel.CALL ->
                    AudioAttributes.USAGE_VOICE_COMMUNICATION to AudioAttributes.CONTENT_TYPE_SPEECH
            }
            ttsInstance.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(contentType)
                    .build()
            )
            Log.d("TtsEngine", "AudioAttributes set to channel=$channel usage=$usage")
        }
    }

    override fun speak(text: String, utteranceId: String) {
        val ttsInstance = tts ?: run {
            Log.e("TtsEngine", "speak() called but TTS not initialized")
            return
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
        if (result == TextToSpeech.ERROR) {
            Log.e("TtsEngine", "speak() returned ERROR for utterance=$utteranceId")
        } else {
            Log.d("TtsEngine", "speak() queued utterance=$utteranceId, result=$result")
        }
    }

    fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {
        val id = "hvc_${utteranceCounter.incrementAndGet()}"
        pendingUtterances[id] = onComplete
        speak(text, id)
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        pendingUtterances.clear()
        tts?.shutdown()
        tts = null
        initOk = false
        voices = emptyList()
    }

    private fun setupProgressListener(ttsInstance: TextToSpeech) {
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TtsEngine", "onStart: $utteranceId")
            }
            override fun onDone(utteranceId: String?) {
                Log.d("TtsEngine", "onDone: $utteranceId")
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(true)
                }
            }
            override fun onError(utteranceId: String?) {
                Log.e("TtsEngine", "onError: $utteranceId")
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(false)
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("TtsEngine", "onError: $utteranceId, code=$errorCode")
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(false)
                }
            }
        })
    }

    private fun setupAudioAttributes(ttsInstance: TextToSpeech) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsInstance.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    private fun queryVoices(ttsInstance: TextToSpeech): List<VoiceInfo> {
        val allVoices = ttsInstance.voices ?: return emptyList()
        return allVoices
            .filter { it.locale.language.equals("en", ignoreCase = true) }
            .map { voice ->
                val isNetwork = voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) ?: false
                VoiceInfo(
                    name = voice.name,
                    localeDisplayName = voice.locale.getDisplayName(voice.locale),
                    localeTag = voice.locale.toLanguageTag(),
                    quality = voice.quality,
                    latency = voice.latency,
                    requiresNetwork = isNetwork,
                    genderLabel = inferGender(voice.name),
                    description = generateDescription(voice.name, isNetwork),
                    isSpecial = isSpecialVoice(voice.name, isNetwork, voice.quality)
                )
            }
            .sortedWith(
                compareBy({ it.localeDisplayName }, { it.name })
            )
    }

    private fun inferGender(name: String): String? {
        val lower = name.lowercase()
        return when {
            lower.contains("male") && !lower.contains("female") -> "Male"
            lower.contains("female") -> "Female"
            else -> null
        }
    }

    private fun generateDescription(name: String, isNetwork: Boolean): String {
        val parts = name.split("-")
        // Try to find the voice variant identifier (e.g. "sfg" in en-us-x-sfg-local)
        val variantPart = parts.firstOrNull { it.length == 3 && it != "eng" && it != "usa" && it != "gbr" && !it.startsWith("en") }?.uppercase() 
            ?: parts.getOrNull(3)?.uppercase() 
            ?: "STANDARD"
            
        val type = if (isNetwork) "Premium Cloud" else "Local"
        return "$type Voice • Variant $variantPart"
    }

    private fun isSpecialVoice(name: String, isNetwork: Boolean, quality: Int): Boolean {
        // Consider network voices or high quality voices as special
        return isNetwork || quality >= 400 || name.contains("-network")
    }
}
