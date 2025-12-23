package com.yral.shared.features.chat.ui.wall

import androidx.compose.ui.graphics.Color

data class InfluencerCardStyle(
    val solidColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

@Suppress("MagicNumber")
val influencerCardStyles =
    listOf(
        InfluencerCardStyle(
            solidColor = Color(0xFF3D8EFF),
            gradientStart = Color(0xFF3D8EFF),
            gradientEnd = Color(0xFF3D8EFF),
        ),
        InfluencerCardStyle(
            solidColor = Color(0xFFF14331),
            gradientStart = Color(0xFFF14331),
            gradientEnd = Color(0xFFF14331),
        ),
        InfluencerCardStyle(
            solidColor = Color(0xFFAD005E),
            gradientStart = Color(0xFFAD005E),
            gradientEnd = Color(0xFFAD005E),
        ),
        InfluencerCardStyle(
            solidColor = Color(0xFFDA8100),
            gradientStart = Color(0xFFDA8100),
            gradientEnd = Color(0xFFDA8100),
        ),
        InfluencerCardStyle(
            solidColor = Color(0xFF0F5132),
            gradientStart = Color(0xFF0F5132),
            gradientEnd = Color(0xFF0F5132),
        ),
        InfluencerCardStyle(
            solidColor = Color(0xFF6A740F),
            gradientStart = Color(0xFF6A740F),
            gradientEnd = Color(0xFF6A740F),
        ),
    )
