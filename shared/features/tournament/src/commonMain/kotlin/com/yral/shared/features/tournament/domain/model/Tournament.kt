package com.yral.shared.features.tournament.domain.model

data class Tournament(
    val id: String,
    val title: String,
    val totalPrizePool: Int,
    val participantsLabel: String,
    val scheduleLabel: String,
    val status: TournamentStatus,
    val participationState: TournamentParticipationState,
    val prizeBreakdown: List<PrizeBreakdownRow>,
)
