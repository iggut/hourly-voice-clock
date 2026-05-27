package com.hourlyvoiceclock.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.SettingsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first
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
    suspend fun switchEngine(enginePackage: String?): Boolean
    fun getEngines(): List<TtsEngineInfo>
}

data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isInstalled: Boolean
)

class AndroidTtsEngine(context: Context) : TtsEngine {

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext
    private var voices: List<VoiceInfo> = emptyList()
    private var initOk = false
    private val utteranceCounter = AtomicInteger(0)
    private val pendingUtterances = mutableMapOf<String, (Boolean) -> Unit>()
    private var currentEnginePackage: String? = null

    override suspend fun initialize(): Boolean {
        if (initOk) return true
        
        // Load the saved engine package name if we haven't set it yet
        if (currentEnginePackage == null) {
            try {
                val repository = SettingsRepository(appContext)
                currentEnginePackage = repository.settings.first().selectedTtsEnginePackage
            } catch (e: Exception) {
                Log.e("TtsEngine", "Failed to load saved TTS engine package", e)
            }
        }

        val success = suspendCancellableCoroutine { continuation ->
            val initListener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val instance = tts ?: run {
                        continuation.resume(false)
                        return@OnInitListener
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
            
            tts = if (currentEnginePackage.isNullOrBlank()) {
                TextToSpeech(appContext, initListener)
            } else {
                TextToSpeech(appContext, initListener, currentEnginePackage)
            }
        }
        return success
    }

    override suspend fun switchEngine(enginePackage: String?): Boolean {
        shutdown()
        currentEnginePackage = enginePackage
        return initialize()
    }

    override fun getEngines(): List<TtsEngineInfo> {
        val ttsInstance = tts ?: return emptyList()
        val installed = ttsInstance.engines.map {
            TtsEngineInfo(
                packageName = it.name,
                label = it.label,
                isInstalled = true
            )
        }
        
        // Define standard popular engines to show even if not installed
        val knownEngines = listOf(
            TtsEngineInfo("com.google.android.tts", "Speech Services by Google", false),
            TtsEngineInfo("com.redzoc.ramees.tts.espeak", "eSpeak NG", false),
            TtsEngineInfo("com.github.olga_yakovleva.rhvoice.android", "RHVoice", false)
        )
        
        val result = installed.toMutableList()
        val existingPackageNames = installed.mapTo(HashSet()) { it.packageName }
        for (known in knownEngines) {
            if (existingPackageNames.add(known.packageName)) {
                result.add(known)
            }
        }
        return result
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
        val sorted = allVoices.sortedWith(compareBy({ it.locale.getDisplayName(it.locale) }, { it.name }))
            
        val countryCounters = mutableMapOf<String, Int>()
        
        return sorted.map { voice ->
            val isNetwork = voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) ?: false
            val country = voice.locale.displayCountry.ifBlank { voice.locale.displayLanguage }
            val count = countryCounters.getOrDefault(country, 0) + 1
            countryCounters[country] = count
            
            VoiceInfo(
                name = voice.name,
                localeDisplayName = country,
                localeTag = voice.locale.toLanguageTag(),
                quality = voice.quality,
                latency = voice.latency,
                requiresNetwork = isNetwork,
                genderLabel = inferGender(voice.name),
                description = "$country Voice $count",
                isSpecial = false
            )
        }
    }

    private val GENDER_MAP = mapOf(
        // US English
        "sfg" to "Female",
        "iom" to "Male",
        "iua" to "Female",
        "rgf" to "Male",
        "tfg" to "Female",
        "lpf" to "Female",
        "rtc" to "Male",
        "tpf" to "Female",
        "jdf" to "Female",
        "iol" to "Male",
        
        // UK English
        "rjs" to "Male",
        "fis" to "Female",
        "gdb" to "Male",
        "gbd" to "Male",
        "gba" to "Female",
        
        // Australian English
        "afh" to "Female",
        "afp" to "Female",
        "ahp" to "Male",
        "aud" to "Male",
        
        // Indian English
        "cxx" to "Female",
        "ene" to "Male",
        "iie" to "Female",
        "iif" to "Male",
        
        // Irish English
        "lcf" to "Female",
        
        // South African English
        "nfc" to "Female",

        // RHVoice voices (English, Russian, etc.)
        "alan" to "Male",
        "bdl" to "Male",
        "clb" to "Female",
        "slt" to "Female",
        "aleksandr" to "Male",
        "anna" to "Female",
        "elena" to "Female",
        "irina" to "Female",
        "spika" to "Female",
        "yuri" to "Male",
        "artemiy" to "Male",
        "tatiana" to "Female",
        "victoria" to "Female",
        "vitaliy" to "Male"
    )

    private fun inferGender(name: String): String? {
        val lower = name.lowercase()
        if (lower.contains("male") && !lower.contains("female")) return "Male"
        if (lower.contains("female")) return "Female"
        
        val segments = lower.split(Regex("[\\-_#\\s]"))
        for (segment in segments) {
            val gender = GENDER_MAP[segment]
            if (gender != null) return gender
        }
        
        return null
    }
}
