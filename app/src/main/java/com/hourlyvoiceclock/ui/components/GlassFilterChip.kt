package com.hourlyvoiceclock.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hourlyvoiceclock.ui.theme.GlassShapes

@Composable
fun GlassFilterChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val fadeColor = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            // ⚡ Bolt: Use drawWithCache instead of drawWithContent to pre-allocate
            // Brush objects (gradients), avoiding re-allocation on every single scroll frame
            .drawWithCache {
                val fadeWidth = 24.dp.toPx()
                val forwardBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, fadeColor),
                    startX = size.width - fadeWidth,
                    endX = size.width
                )
                val backwardBrush = Brush.horizontalGradient(
                    colors = listOf(fadeColor, Color.Transparent),
                    startX = 0f,
                    endX = fadeWidth
                )
                onDrawWithContent {
                    drawContent()
                    if (scrollState.canScrollForward) {
                        drawRect(brush = forwardBrush)
                    }
                    if (scrollState.canScrollBackward) {
                        drawRect(brush = backwardBrush)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        modifier = modifier,
        shape = GlassShapes.Chip,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
