package com.yral.shared.features.tournament.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed class TournamentStatus {
    @OptIn(ExperimentalTime::class)
    data class Upcoming(
        val startTime: Instant,
    ) : TournamentStatus()

    @OptIn(ExperimentalTime::class)
    data class Live(
        val endTime: Instant,
    ) : TournamentStatus()

    data object Ended : TournamentStatus()
}
