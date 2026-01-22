package com.yral.shared.features.tournament.domain.model

data class Tournament(
    val id: String,
    val title: String,
    val type: TournamentType = TournamentType.SMILEY,
    val totalPrizePool: Int,
    val participantsLabel: String,
    val scheduleLabel: String,
    val status: TournamentStatus,
    val participationState: TournamentParticipationState,
    val prizeBreakdown: List<PrizeBreakdownRow>,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val entryCost: Int,
    val isRegistered: Boolean,
    val userDiamonds: Int,
) {
    /**
     * Calculate initial diamonds for the game (1.5x entry cost).
     */
    @Suppress("MagicNumber")
    val initialDiamonds: Int get() = (entryCost * 1.5).toInt()
}
