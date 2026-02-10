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
    val entryCostCredits: Int,
    val isRegistered: Boolean,
    val userDiamonds: Int,
    val isDaily: Boolean = false,
    val dailyTimeLimitMs: Long = 0,
    val remainingTimeMs: Long? = null,
) {
    /**
     * Calculate initial diamonds for the game.
     * Daily tournaments use a fixed value from config, regular tournaments use 1.5x entry cost.
     */
    @Suppress("MagicNumber")
    val initialDiamonds: Int
        get() =
            if (isDaily) {
                DAILY_INITIAL_DIAMONDS
            } else {
                (entryCost * 1.5).toInt()
            }

    companion object {
        private const val DAILY_INITIAL_DIAMONDS = 20
    }
}
