package com.hourlyvoiceclock.ui.voicesettings

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun presetGradientFor(id: String): Brush = when (id) {
    "preset_robot", "espeak_robot" -> Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2)))
    "preset_freeman" -> Brush.horizontalGradient(listOf(Color(0xFF78350F), Color(0xFFD97706)))
    "preset_giant", "espeak_monster" -> Brush.horizontalGradient(listOf(Color(0xFF6B21A8), Color(0xFF5B21B6)))
    "preset_chipmunk", "espeak_chipmunk" -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
    "preset_goblin", "espeak_alien" -> Brush.horizontalGradient(listOf(Color(0xFF84CC16), Color(0xFF65A30D)))
    "preset_redneck", "espeak_dwarf" -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C)))
    "preset_baby", "espeak_baby", "espeak_cartoon" -> Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFD946EF)))
    "preset_donald" -> Brush.horizontalGradient(listOf(Color(0xFFEAB308), Color(0xFFCA8A04)))
    "preset_nerdy", "espeak_wizard" -> Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
    "preset_slowmo", "espeak_evil" -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
    "espeak_deep" -> Brush.horizontalGradient(listOf(Color(0xFF78716C), Color(0xFF57534E)))
    "espeak_ghost" -> Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
    "espeak_robot_female" -> Brush.horizontalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0D9488)))
    else -> Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
}
