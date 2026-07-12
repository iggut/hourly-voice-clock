package com.hourlyvoiceclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Modern Vibrant Color Palette
val Indigo80 = Color(0xFFA5B4FC) // Vibrant soft indigo
val IndigoGrey80 = Color(0xFFCBD5E1) // Clean slate grey
val Teal80 = Color(0xFF6EE7B7) // Mint teal

val Indigo40 = Color(0xFF4F46E5) // Royal Indigo
val IndigoGrey40 = Color(0xFF475569) // Slate grey
val Teal40 = Color(0xFF0D9488) // Vibrant teal

// Background Gradient Colors
val DarkBgStart = Color(0xFF080914)
val DarkBgEnd = Color(0xFF131527)
val LightBgStart = Color(0xFFF8FAFC)
val LightBgEnd = Color(0xFFEEF2F6)

// Card & Glassmorphic Highlights
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val GlassBorderLight = Color(0xFF000000).copy(alpha = 0.06f)
val GlassBgDark = Color(0xFF1E293B).copy(alpha = 0.6f)
val GlassBgLight = Color(0xFFFFFFFF).copy(alpha = 0.5f)

// Opaque surfaces for modal dialogs (AlertDialog must not use translucent theme surface)
val DialogSurfaceDark = Color(0xFF1E293B)
val DialogSurfaceLight = Color(0xFFFFFFFF)
val DialogOnSurfaceDark = Color(0xFFF1F5F9)
val DialogOnSurfaceLight = Color(0xFF0F172A)

// Semantic badge / status accents
val AccentFemale = Color(0xFFEC4899)
val AccentMale = Color(0xFF3B82F6)
val AccentCloud = Color(0xFFEAB308)
val AccentOffline = Color(0xFF10B981)

/** Whether Material You dynamic color is active for this composition. */
val LocalDynamicColor = compositionLocalOf { false }

@Composable
fun glassContainerColor(): Color {
    return if (LocalDynamicColor.current) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    } else if (isSystemInDarkTheme()) {
        GlassBgDark
    } else {
        GlassBgLight
    }
}

@Composable
fun glassBorderColor(): Color {
    return if (LocalDynamicColor.current) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    } else if (isSystemInDarkTheme()) {
        GlassBorderDark
    } else {
        GlassBorderLight
    }
}
