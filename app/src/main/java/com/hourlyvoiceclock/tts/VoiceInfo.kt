package com.hourlyvoiceclock.tts

data class VoiceInfo(
    val name: String,
    val localeDisplayName: String,
    val localeTag: String,
    val quality: Int,
    val latency: Int,
    val requiresNetwork: Boolean,
    val genderLabel: String?,
    val description: String,
    val isSpecial: Boolean
)
