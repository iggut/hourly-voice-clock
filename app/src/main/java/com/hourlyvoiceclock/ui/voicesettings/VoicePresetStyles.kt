package com.hourlyvoiceclock.ui.voicesettings

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hourlyvoiceclock.R

fun presetGradientFor(id: String): Brush = when (id) {
    "preset_robot", "espeak_robot" -> Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2)))
    "preset_narrator" -> Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
    "preset_giant", "espeak_monster" -> Brush.horizontalGradient(listOf(Color(0xFF6B21A8), Color(0xFF5B21B6)))
    "preset_chipmunk" -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
    "preset_paris" -> Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF9333EA)))
    "preset_radio" -> Brush.horizontalGradient(listOf(Color(0xFF92400E), Color(0xFF57534E)))
    "preset_baby", "espeak_cartoon" -> Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFD946EF)))
    "preset_cartoon" -> Brush.horizontalGradient(listOf(Color(0xFFEAB308), Color(0xFFCA8A04)))
    "preset_professor", "espeak_deep" -> Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
    "preset_slowmo" -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
    "preset_whisper" -> Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B)))
    "preset_news" -> Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)))
    "preset_auctioneer" -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C)))
    "preset_drill" -> Brush.horizontalGradient(listOf(Color(0xFF78716C), Color(0xFF44403C)))
    "preset_storyteller" -> Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E)))
    "preset_sparkle" -> Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFA855F7)))
    "espeak_alien" -> Brush.horizontalGradient(listOf(Color(0xFF84CC16), Color(0xFF65A30D)))
    "espeak_ghost" -> Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
    else -> Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
}

@StringRes
fun specialVoiceTagLabelRes(tag: SpecialVoiceTag): Int = when (tag) {
    SpecialVoiceTag.FUN -> R.string.special_tag_fun
    SpecialVoiceTag.CHARACTER -> R.string.special_tag_character
    SpecialVoiceTag.ACCENT -> R.string.special_tag_accent
    SpecialVoiceTag.ESPEAK -> R.string.special_tag_espeak
}
