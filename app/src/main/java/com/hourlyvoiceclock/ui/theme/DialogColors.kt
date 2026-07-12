package com.hourlyvoiceclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Opaque dialog surfaces — theme [surface] is intentionally translucent for glass cards. */
@Composable
fun dialogContainerColor(): Color {
    if (LocalDynamicColor.current) {
        // Force fully opaque surface for dialogs even when scheme surface is translucent.
        val surface = MaterialTheme.colorScheme.surface
        return surface.copy(alpha = 1f)
    }
    return if (isSystemInDarkTheme()) DialogSurfaceDark else DialogSurfaceLight
}

@Composable
fun dialogContentColor(): Color {
    if (LocalDynamicColor.current) {
        val onSurface = MaterialTheme.colorScheme.onSurface
        return if (onSurface.alpha < 1f) onSurface.copy(alpha = 1f) else onSurface
    }
    return if (isSystemInDarkTheme()) DialogOnSurfaceDark else DialogOnSurfaceLight
}
