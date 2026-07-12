package com.hourlyvoiceclock.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hourlyvoiceclock.ui.theme.DarkBgEnd
import com.hourlyvoiceclock.ui.theme.DarkBgStart
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.LightBgEnd
import com.hourlyvoiceclock.ui.theme.LightBgStart
import com.hourlyvoiceclock.ui.theme.LocalDynamicColor
import com.hourlyvoiceclock.ui.theme.glassBorderColor
import com.hourlyvoiceclock.ui.theme.glassContainerColor

/**
 * Home hero card. On API 31+ draws a blurred gradient layer under a light glass
 * tint so the clock sits on softened backdrop glass; older APIs use plain glass fill.
 */
@Composable
fun HeroGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = GlassShapes.Hero
    val glass = glassContainerColor()
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isDark = isSystemInDarkTheme()
    val backdropBrush = if (LocalDynamicColor.current) {
        val bg = MaterialTheme.colorScheme.background
        Brush.verticalGradient(
            listOf(bg, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (isDark) DarkBgStart else LightBgStart,
                if (isDark) DarkBgEnd else LightBgEnd
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, glassBorderColor())
    ) {
        Box(modifier = Modifier.clip(shape)) {
            if (supportsBlur) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(28.dp)
                        .background(backdropBrush)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(glass.copy(alpha = 0.42f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(glass)
                )
            }
            Column(
                modifier = Modifier.padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}
