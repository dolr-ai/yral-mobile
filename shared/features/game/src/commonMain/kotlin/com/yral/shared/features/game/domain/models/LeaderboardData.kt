package com.yral.shared.features.game.domain.models

data class LeaderboardData(
    val userRow: LeaderboardItem?,
    val topRows: List<LeaderboardItem>,
    val timeLeftMs: Long?,
)
