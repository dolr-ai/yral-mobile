package com.yral.shared.libs.leaderboard.model

enum class LeaderboardMode(
    val showCountDown: Boolean,
    val showHistory: Boolean,
) {
    DAILY(showCountDown = true, showHistory = true),
    ALL_TIME(showCountDown = false, showHistory = false),
}

enum class RewardCurrency {
    YRAL,
    BTC,
}

data class LeaderboardEntry(
    val principalId: String,
    val username: String,
    val profileImageUrl: String,
    val wins: Long,
    val position: Int,
    val reward: Double? = null,
)
