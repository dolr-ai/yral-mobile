package com.yral.shared.features.leaderboard.domain.models

data class LeaderboardData(
    val userRow: LeaderboardItem?,
    val topRows: List<LeaderboardItem>,
    val timeLeftMs: Long?,
)
