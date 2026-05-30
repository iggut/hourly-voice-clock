package com.hourlyvoiceclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Opaque dialog surfaces — theme [surface] is intentionally translucent for glass cards. */
@Composable
fun dialogContainerColor(): Color =
    if (isSystemInDarkTheme()) DialogSurfaceDark else DialogSurfaceLight

@Composable
fun dialogContentColor(): Color =
    if (isSystemInDarkTheme()) DialogOnSurfaceDark else DialogOnSurfaceLight
