package com.hourlyvoiceclock.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

interface TtsEngine {
    suspend fun initialize(): Boolean
    fun getVoices(): List<VoiceInfo>
    fun setVoice(voiceName: String, localeTag: String): Boolean
    fun setLanguage(localeTag: String): Boolean
    fun setPitch(pitch: Float)
    fun setSpeechRate(rate: Float)
    suspend fun speak(text: String): Boolean
    fun shutdown()
}

class AndroidTtsEngine(context: Context) : TtsEngine {

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext
    private var voices: List<VoiceInfo> = emptyList()
    private val utteranceCounter = AtomicInteger(0)
    private val pendingUtterances = mutableMapOf<String, (Boolean) -> Unit>()

    override suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        tts?.let {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                voices = queryVoices()
                setupProgressListener()
                continuation.resume(true)
            } else {
                continuation.resume(false)
            }
        }
    }

    override fun getVoices(): List<VoiceInfo> = voices

    override fun setVoice(voiceName: String, localeTag: String): Boolean {
        if (voiceName.isBlank() || localeTag.isBlank()) return false
        val ttsInstance = tts ?: return false
        val voice = ttsInstance.voices?.find {
            it.name == voiceName && it.locale.toLanguageTag() == localeTag
        } ?: return false
        return ttsInstance.setVoice(voice) == TextToSpeech.SUCCESS
    }

    override fun setLanguage(localeTag: String): Boolean {
        if (localeTag.isBlank()) return false
        val ttsInstance = tts ?: return false
        val locale = Locale.forLanguageTag(localeTag)
        val result = ttsInstance.setLanguage(locale)
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

    override suspend fun speak(text: String): Boolean {
        val result = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { continuation ->
                val ttsInstance = tts ?: run {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                val utteranceId = "hvc_${utteranceCounter.incrementAndGet()}"
                pendingUtterances[utteranceId] = { success -> continuation.resume(success) }

                continuation.invokeOnCancellation {
                    pendingUtterances.remove(utteranceId)
                    ttsInstance.stop()
                }

                val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                } else {
                    @Suppress("DEPRECATION")
                    val params = HashMap<String, String>()
                    params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
                    ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params)
                }

                if (result == TextToSpeech.ERROR) {
                    pendingUtterances.remove(utteranceId)
                    continuation.resume(false)
                }
            }
        }
        return result ?: false
    }

    override fun shutdown() {
        pendingUtterances.clear()
        tts?.shutdown()
        tts = null
    }

    private fun setupProgressListener() {
        val ttsInstance = tts ?: return
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(true)
                }
            }
            override fun onError(utteranceId: String?) {
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(false)
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(false)
                }
            }
        })
    }

    private fun queryVoices(): List<VoiceInfo> {
        val ttsInstance = tts ?: return emptyList()
        val allVoices = ttsInstance.voices ?: return emptyList()
        return allVoices.map { voice ->
            VoiceInfo(
                name = voice.name,
                localeDisplayName = voice.locale.getDisplayName(voice.locale),
                localeTag = voice.locale.toLanguageTag(),
                quality = voice.quality,
                latency = voice.latency,
                requiresNetwork = voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) ?: false,
                genderLabel = inferGender(voice.name)
            )
        }.sortedWith(
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
}
