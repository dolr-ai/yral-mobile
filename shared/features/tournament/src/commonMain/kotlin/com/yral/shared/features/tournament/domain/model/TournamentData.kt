package com.yral.shared.features.tournament.domain.model

/**
 * API-focused tournament data model.
 * This is the raw tournament data from the API, which can be converted to UI models.
 */
data class TournamentData(
    val id: String,
    val title: String,
    val type: TournamentType = TournamentType.SMILEY,
    val date: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val entryCost: Int,
    val entryCostCredits: Int,
    val totalPrizePool: Int,
    val prizeMap: Map<Int, Int>,
    val participantCount: Int,
    val userStats: UserTournamentStats? = null,
)

data class UserTournamentStats(
    val coinsPaid: Int,
    val diamonds: Int,
    val tournamentWins: Int,
    val tournamentLosses: Int,
    val registrationStatus: String,
)
