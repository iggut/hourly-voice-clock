package com.hourlyvoiceclock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.GlassTypography
import com.hourlyvoiceclock.ui.theme.glassBorderColor
import com.hourlyvoiceclock.ui.theme.glassContainerColor

@Composable
fun GlassInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconContentDescription: String? = null,
    containerColor: Color = glassContainerColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    val borderColor = accentColor?.copy(alpha = 0.25f) ?: glassBorderColor()
    Card(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = GlassShapes.Item,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = accentColor ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = GlassTypography.cardSubtitle,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke(this)
        }
    }
}

@Composable
fun GlassInfoBannerColumn(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconContentDescription: String? = null,
    containerColor: Color = glassContainerColor(),
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val borderColor = accentColor?.copy(alpha = 0.25f) ?: glassBorderColor()
    Card(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = GlassShapes.Item,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = accentColor ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            content()
        }
    }
}
