package com.hourlyvoiceclock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radius scale for glass UI surfaces. */
object GlassShapes {
    val Hero = RoundedCornerShape(28.dp)
    val Section = RoundedCornerShape(24.dp)
    val Dashboard = RoundedCornerShape(20.dp)
    val Item = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(12.dp)
    val Badge = RoundedCornerShape(6.dp)
}

/** Spacing rhythm used across glass screens. */
object GlassSpacing {
    val PageHorizontal = 20.dp
    val SectionGap = 20.dp
    val CardInner = 18.dp
    val ListGap = 12.dp
    val BottomSpacer = 30.dp
}
