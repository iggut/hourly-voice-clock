package com.hourlyvoiceclock.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.hourlyvoiceclock.announcer.AudioChannelMapping
import com.hourlyvoiceclock.data.AudioChannel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

interface TtsEngine {
    suspend fun initialize(enginePackage: String?): Boolean
    fun isAvailable(): Boolean
    fun getVoices(): List<VoiceInfo>
    fun setVoice(voiceName: String, localeTag: String): Boolean
    fun setLanguage(localeTag: String): Boolean
    fun setPitch(pitch: Float)
    fun setSpeechRate(rate: Float)
    fun setAudioChannel(channel: AudioChannel)

    /**
     * Apply the given [profile] to this engine in one call.
     *
     * The adapter decides which fields are meaningful: the system
     * engine walks the voice/locale fallback cascade, while the local
     * engine loads the requested model. Returns `true` if a specific
     * voice or model was successfully selected.
     */
    fun configure(profile: VoiceProfile): Boolean

    fun speak(text: String, utteranceId: String)
    fun speakAsync(text: String, onComplete: (Boolean) -> Unit)
    fun stop()
    fun shutdown()
    suspend fun switchEngine(enginePackage: String?): Boolean
    fun getEngines(): List<TtsEngineInfo>
    fun getCurrentEnginePackage(): String?
    fun isEspeakNgEngine(): Boolean
}

data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isInstalled: Boolean
)

class AndroidTtsEngine(context: Context) : TtsEngine {

    companion object {
        private const val TAG = "TtsEngine"
    }

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext
    private var voices: List<VoiceInfo> = emptyList()
    private var initOk = false
    private val utteranceCounter = AtomicInteger(0)
    private val pendingUtterances = ConcurrentHashMap<String, (Boolean) -> Unit>()
    private var currentEnginePackage: String? = null
    private var currentAudioChannel: AudioChannel = AudioChannel.MEDIA

    override suspend fun initialize(enginePackage: String?): Boolean {
        if (initOk && currentEnginePackage == enginePackage) return true

        // A re-init with a different package means the caller is
        // switching engines. Tear down the previous instance.
        if (tts != null) {
            shutdown()
        }
        currentEnginePackage = enginePackage

        val success = suspendCancellableCoroutine { continuation ->
            val initListener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val instance = tts ?: run {
                        continuation.resume(false)
                        return@OnInitListener
                    }
                    voices = queryVoices(instance)
                    setupProgressListener(instance)
                    setupAudioAttributes(instance, currentAudioChannel)
                    initOk = true
                    continuation.resume(true)
                } else {
                    Log.e(TAG, "TTS init failed with status=$status")
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
        return initialize(enginePackage)
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
            TtsEngineInfo("com.redzoc.ramees.tts.espeak", "eSpeak NG", false)
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

    override fun getCurrentEnginePackage(): String? = currentEnginePackage

    override fun isEspeakNgEngine(): Boolean {
        return currentEnginePackage?.contains("espeak", ignoreCase = true) == true ||
               currentEnginePackage == "com.redzoc.ramees.tts.espeak"
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
        Log.d(TAG, "setVoice(${voice.name}) = $result")
        return result == TextToSpeech.SUCCESS
    }

    override fun setLanguage(localeTag: String): Boolean {
        if (localeTag.isBlank()) return false
        val ttsInstance = tts ?: return false
        val locale = Locale.forLanguageTag(localeTag)
        val result = ttsInstance.setLanguage(locale)
        Log.d(TAG, "setLanguage($localeTag) = $result")
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
        currentAudioChannel = channel
        setupAudioAttributes(ttsInstance, channel)
    }

    override fun configure(profile: VoiceProfile): Boolean {
        val voiceSet = selectVoiceOrLocale(
            voiceName = profile.voiceName,
            savedLocale = profile.localeTag,
            setVoice = { name, tag -> setVoice(name, tag) },
            setLanguage = { tag -> setLanguage(tag) }
        )
        setPitch(profile.pitch)
        setSpeechRate(profile.speechRate)
        setAudioChannel(profile.audioChannel)
        return voiceSet
    }

    override fun speak(text: String, utteranceId: String) {
        val ttsInstance = tts ?: run {
            Log.e(TAG, "speak() called but TTS not initialized")
            return
        }
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            @Suppress("DEPRECATION")
            putString(TextToSpeech.Engine.KEY_PARAM_STREAM, audioStreamFor(currentAudioChannel).toString())
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, hashMapOf(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId,
                TextToSpeech.Engine.KEY_PARAM_STREAM to audioStreamFor(currentAudioChannel).toString()
            ))
        }
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "speak() returned ERROR for utterance=$utteranceId")
        } else {
            Log.d(TAG, "speak() queued utterance=$utteranceId, result=$result")
        }
    }

    override fun speakAsync(text: String, onComplete: (Boolean) -> Unit) {
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
                Log.d(TAG, "onStart: $utteranceId")
            }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "onDone: $utteranceId")
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(true)
                }
            }
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "onError: $utteranceId")
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(false)
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "onError: $utteranceId, code=$errorCode")
                utteranceId?.let { id ->
                    pendingUtterances.remove(id)?.invoke(false)
                }
            }
        })
    }

    private fun setupAudioAttributes(ttsInstance: TextToSpeech, channel: AudioChannel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val spec = AudioChannelMapping.specOf(channel)
            ttsInstance.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(spec.usage)
                    .setContentType(spec.contentType)
                    .build()
            )
            Log.d(TAG, "AudioAttributes set to channel=$channel usage=${spec.usage} stream=${spec.stream}")
        }
    }

    private fun audioStreamFor(channel: AudioChannel): Int =
        AudioChannelMapping.specOf(channel).stream

    private fun queryVoices(ttsInstance: TextToSpeech): List<VoiceInfo> {
        val allVoices = ttsInstance.voices ?: return emptyList()
        val sorted = allVoices
            .asSequence()
            .filter { it.locale.isSupportedAnnouncementLocale() }
            .sortedWith(compareBy<Voice>({ it.locale.getDisplayName(it.locale) }, { it.name }))
            .toList()

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

    private fun Locale.isSupportedAnnouncementLocale(): Boolean {
        val normalizedLanguage = language.lowercase(Locale.ROOT)
        return normalizedLanguage == Locale.ENGLISH.language ||
                normalizedLanguage == Locale.FRENCH.language ||
                normalizedLanguage == "eng" ||
                normalizedLanguage == "fra" ||
                normalizedLanguage == "fre"
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

    private val GENDER_SPLIT_REGEX = Regex("[\\-_#\\s]")

    private fun inferGender(name: String): String? {
        val lower = name.lowercase()
        if (lower.contains("male") && !lower.contains("female")) return "Male"
        if (lower.contains("female")) return "Female"

        val segments = lower.split(GENDER_SPLIT_REGEX)
        for (segment in segments) {
            val gender = GENDER_MAP[segment]
            if (gender != null) return gender
        }

        return null
    }
}

private const val FALLBACK_LOCALE = "en-US"

/**
 * Walk the voice-selection fallback cascade used by [AndroidTtsEngine.configure].
 *
 * Exposed as an internal file-level function so the policy can be unit-tested
 * without constructing a real [android.speech.tts.TextToSpeech] instance.
 * It is not part of the public [TtsEngine] seam.
 */
internal fun selectVoiceOrLocale(
    voiceName: String?,
    savedLocale: String?,
    deviceDefaultLocale: String = Locale.getDefault().toLanguageTag(),
    setVoice: (String, String) -> Boolean,
    setLanguage: (String) -> Boolean
): Boolean {
    if (!voiceName.isNullOrBlank()) {
        val localeTag = savedLocale?.takeIf { it.isNotBlank() } ?: ""
        if (setVoice(voiceName, localeTag)) return true
    }
    if (!savedLocale.isNullOrBlank() && setLanguage(savedLocale)) return true
    if (deviceDefaultLocale.isNotBlank() && setLanguage(deviceDefaultLocale)) return true
    // Last resort. setLanguage is allowed to return false here — at
    // least we tried to give the engine *something* to speak.
    setLanguage(FALLBACK_LOCALE)
    return false
}
