package com.yral.shared.features.tournament.domain.model

data class TournamentLeaderboard(
    val tournamentId: String,
    val status: String,
    val topRows: List<LeaderboardRow>,
    val userRow: LeaderboardRow?,
    val prizeMap: Map<Int, Int>,
)

data class LeaderboardRow(
    val principalId: String,
    val username: String?,
    val wins: Int,
    val losses: Int,
    val position: Int,
    val prize: Int?,
)
