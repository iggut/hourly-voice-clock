package com.hourlyvoiceclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hourlyvoiceclock.ui.theme.DarkBgEnd
import com.hourlyvoiceclock.ui.theme.DarkBgStart
import com.hourlyvoiceclock.ui.theme.GlassTypography
import com.hourlyvoiceclock.ui.theme.LightBgEnd
import com.hourlyvoiceclock.ui.theme.LightBgStart
import com.hourlyvoiceclock.ui.theme.LocalDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassScreen(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgGradient = if (LocalDynamicColor.current) {
        val bg = MaterialTheme.colorScheme.background
        Brush.verticalGradient(
            colors = listOf(
                bg,
                if (isDark) bg.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (isDark) DarkBgStart else LightBgStart,
                if (isDark) DarkBgEnd else LightBgEnd
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                if (snackbarHostState != null) {
                    SnackbarHost(hostState = snackbarHostState)
                }
            },
            floatingActionButton = floatingActionButton,
            topBar = {
                if (title != null) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = title, style = GlassTypography.screenTitle)
                        },
                        navigationIcon = {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.Unspecified,
                            navigationIconContentColor = Color.Unspecified
                        )
                    )
                }
            },
            content = content
        )
    }
}

@Composable
fun Modifier.glassPagePadding(padding: PaddingValues): Modifier =
    this
        .padding(padding)
        .padding(horizontal = com.hourlyvoiceclock.ui.theme.GlassSpacing.PageHorizontal)
