package com.yral.shared.features.game.domain.models

data class LeaderboardHistoryError(
    val code: LeaderboardErrorCodes,
    val message: String,
    val throwable: Throwable? = null,
)

typealias LeaderboardHistory = List<LeaderboardHistoryDay>

data class LeaderboardHistoryDay(
    val date: String,
    val topRows: List<LeaderboardItem>,
    val userRow: LeaderboardItem?,
)
