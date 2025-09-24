package com.yral.shared.features.leaderboard.domain.models

data class LeaderboardData(
    val userRow: LeaderboardItem?,
    val topRows: List<LeaderboardItem>,
    val timeLeftMs: Long?,
    val rewardCurrency: RewardCurrency? = null,
    val rewardCurrencyCode: String? = null,
    val rewardsTable: Map<RewardPosition, Double>? = null,
)

enum class RewardCurrency {
    YRAL,
    BTC,
}

enum class RewardPosition {
    FIRST,
    SECOND,
    THIRD,
    OTHER,
}
