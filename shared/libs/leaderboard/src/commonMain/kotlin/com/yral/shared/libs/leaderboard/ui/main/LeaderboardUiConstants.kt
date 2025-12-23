package com.yral.shared.libs.leaderboard.ui.main

import androidx.compose.ui.graphics.Color

@Suppress("MagicNumber")
object LeaderboardUiConstants {
    val LEADERBOARD_HEADER_WEIGHTS = listOf(0.27f, 0.32f, 0.40f, 0.29f)
    val LEADERBOARD_HEADER_WEIGHTS_FOLD = listOf(0.27f, 0.34f, 0.40f, 0.27f)
    val LEADERBOARD_ROW_WEIGHTS = listOf(0.27f, 0.35f, 0.40f, 0.26f)
    const val MAX_CHAR_OF_NAME = 9
    const val COUNT_DOWN_BG_ALPHA = 0.8f
    const val COUNT_DOWN_ANIMATION_DURATION = 500
    const val COUNT_DOWN_BORDER_ANIMATION_DURATION = 300
    val YELLOW_BRUSH =
        listOf(
            Color(0x00FFC842).copy(alpha = 0f),
            Color(0xFFF6B517).copy(alpha = 0.7f),
        )
    val PURPLE_BRUSH =
        listOf(
            Color(0x00706EBB).copy(alpha = 0f),
            Color(0xFF7573BD).copy(alpha = 0.7f),
        )

    const val CONFETTI_SCALE = 2.5f
    const val NO_OF_CONFETTI = 5
    const val CONFETTI_SIZE_FACTOR = 3
    const val CONFETTI_ITERATIONS = 0
}
