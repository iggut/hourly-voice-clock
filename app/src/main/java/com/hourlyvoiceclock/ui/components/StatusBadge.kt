package com.hourlyvoiceclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.GlassTypography

@Composable
fun StatusBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    Text(
        text = text,
        style = GlassTypography.badgeLabel,
        color = contentColor,
        modifier = modifier
            .clip(GlassShapes.Badge)
            .background(accent)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun SoftStatusBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = GlassTypography.badgeLabel,
        color = accent,
        modifier = modifier
            .clip(GlassShapes.Badge)
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
