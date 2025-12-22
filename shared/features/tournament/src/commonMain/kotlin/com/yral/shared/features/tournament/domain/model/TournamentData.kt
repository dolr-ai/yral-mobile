package com.yral.shared.features.tournament.domain.model

/**
 * API-focused tournament data model.
 * This is the raw tournament data from the API, which can be converted to UI models.
 */
data class TournamentData(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val entryCost: Int,
    val totalPrizePool: Int,
    val status: String,
    val prizeMap: Map<Int, Int>,
    val participantCount: Int,
    val userStats: UserTournamentStats? = null,
)

data class UserTournamentStats(
    val coinsPaid: Int,
    val tournamentWins: Int,
    val tournamentLosses: Int,
    val registrationStatus: String,
)
