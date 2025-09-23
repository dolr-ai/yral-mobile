package com.yral.shared.features.leaderboard.ui.leaderboard.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.bronze_border
import yral_mobile.shared.features.leaderboard.generated.resources.bronze_ring
import yral_mobile.shared.features.leaderboard.generated.resources.golden_border
import yral_mobile.shared.features.leaderboard.generated.resources.golden_ring
import yral_mobile.shared.features.leaderboard.generated.resources.silver_border
import yral_mobile.shared.features.leaderboard.generated.resources.silver_ring
import yral_mobile.shared.libs.designsystem.generated.resources.bronze_gradient
import yral_mobile.shared.libs.designsystem.generated.resources.golden_gradient
import yral_mobile.shared.libs.designsystem.generated.resources.silver_gradient
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

object LeaderboardHelpers {
    const val POS_GOLD = 1
    const val POS_SILVER = 2
    const val POS_BRONZE = 3

    fun getTextDecoration(position: Int): DrawableResource? =
        when (position) {
            POS_GOLD -> DesignRes.drawable.golden_gradient
            POS_SILVER -> DesignRes.drawable.silver_gradient
            POS_BRONZE -> DesignRes.drawable.bronze_gradient
            else -> null
        }

    @Suppress("MagicNumber")
    fun getBrush(
        position: Int,
        textSize: IntSize,
    ): Brush? {
        val width = textSize.width.toFloat().coerceAtLeast(1f)
        val height = textSize.height.toFloat().coerceAtLeast(1f)

        val scaleX = width / 50f
        val scaleY = height / 50f
        val radiusScale = maxOf(width, height) / 50f

        return when (position) {
            POS_GOLD ->
                Brush.radialGradient(
                    0f to Color(0xFFBF760B),
                    0.413f to Color(0xFFFFE89F),
                    1f to Color(0xFFC38F00),
                    center = Offset(0f, 4.5f * scaleY),
                    radius = 99.086f * radiusScale,
                )

            POS_SILVER ->
                Brush.radialGradient(
                    0f to Color(0xFF2F2F30),
                    0.413f to Color(0xFFFFFFFF),
                    1f to Color(0xFF4B4B4B),
                    center = Offset(0f, 0f),
                    radius = 91.219f * radiusScale,
                )

            POS_BRONZE ->
                Brush.radialGradient(
                    0f to Color(0xFF6D4C35),
                    0.413f to Color(0xFFDBA374),
                    1f to Color(0xFF9F7753),
                    center = Offset(1f * scaleX, 2f * scaleY),
                    radius = 98.995f * radiusScale,
                )

            else -> null
        }
    }

    fun getProfileImageRing(position: Int): DrawableResource? =
        when (position) {
            POS_GOLD -> Res.drawable.golden_ring
            POS_SILVER -> Res.drawable.silver_ring
            POS_BRONZE -> Res.drawable.bronze_ring
            else -> null
        }

    fun getUserBriefBorder(position: Int): DrawableResource? =
        when (position) {
            POS_GOLD -> Res.drawable.golden_border
            POS_SILVER -> Res.drawable.silver_border
            POS_BRONZE -> Res.drawable.bronze_border
            else -> null
        }

    fun getTrophyImageWidth(position: Int): Dp =
        when (position) {
            POS_GOLD -> 66.75.dp
            POS_SILVER -> 45.75.dp
            POS_BRONZE -> 45.dp
            else -> 45.dp
        }

    fun getTrophyImageHeight(position: Int): Dp =
        when (position) {
            POS_GOLD -> 146.dp
            POS_SILVER -> 106.dp
            POS_BRONZE -> 91.dp
            else -> 91.dp
        }

    fun getTrophyImageOffset(
        position: Int,
        isProfileImageVisible: Boolean,
    ) = if (isProfileImageVisible) {
        when (position) {
            POS_GOLD -> 20.dp
            POS_SILVER -> 13.75.dp
            POS_BRONZE -> 17.49.dp
            else -> 17.49.dp
        }
    } else {
        48.dp
    }

    const val PROFILE_IMAGE_SIZE = 25f
    const val MAX_USERS_WITH_DUPLICATE_RANK = 4
    const val MAX_USERS_PRINCIPAL_LENGTH = 4
}
