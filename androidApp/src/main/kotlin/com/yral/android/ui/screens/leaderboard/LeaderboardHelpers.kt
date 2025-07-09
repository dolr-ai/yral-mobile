package com.yral.android.ui.screens.leaderboard

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.android.R

object LeaderboardHelpers {
    fun getTextDecoration(rank: Int): Int =
        when (rank) {
            0 -> R.drawable.golden_gradient
            1 -> R.drawable.silver_gradient
            2 -> R.drawable.bronze_gradient
            else -> 0
        }

    fun getProfileImageRing(rank: Int): Int =
        when (rank) {
            0 -> R.drawable.golden_ring
            1 -> R.drawable.silver_ring
            2 -> R.drawable.bronze_ring
            else -> 0
        }

    fun getUserBriefBorder(rank: Int): Int =
        when (rank) {
            0 -> R.drawable.golden_border
            1 -> R.drawable.silver_border
            2 -> R.drawable.bronze_border
            else -> 0
        }

    fun getTrophyImageWidth(rank: Int): Dp =
        when (rank) {
            0 -> 66.75.dp
            1 -> 45.75.dp
            2 -> 45.dp
            else -> 45.dp
        }

    fun getTrophyImageHeight(rank: Int): Dp =
        when (rank) {
            0 -> 146.dp
            1 -> 106.dp
            2 -> 91.dp
            else -> 91.dp
        }

    fun getTrophyImageOffset(
        rank: Int,
        isProfileImageVisible: Boolean,
    ) = if (isProfileImageVisible) {
        when (rank) {
            0 -> 20.dp
            1 -> 13.75.dp
            2 -> 17.49.dp
            else -> 17.49.dp
        }
    } else {
        48.dp
    }

    const val PROFILE_IMAGE_SIZE = 25f
    const val MAX_USERS_WITH_DUPLICATE_RANK = 4
    const val MAX_USERS_PRINCIPAL_LENGTH = 4
}
