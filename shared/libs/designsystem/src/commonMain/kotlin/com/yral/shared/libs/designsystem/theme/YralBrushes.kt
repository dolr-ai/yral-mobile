package com.yral.shared.libs.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Suppress("MagicNumber")
object YralBrushes {
    val GoldenTextBrush =
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFCC00), Color(0xFFDA8100)),
            start = Offset(23.197f, -18.929f),
            end = Offset(71.175f, -15.036f),
        )
}
