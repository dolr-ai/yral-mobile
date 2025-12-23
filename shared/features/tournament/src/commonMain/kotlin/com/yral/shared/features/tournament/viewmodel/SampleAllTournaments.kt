package com.yral.shared.features.tournament.viewmodel

import com.yral.shared.features.tournament.domain.model.PrizeBreakdownRow
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
internal fun sampleAllTournaments(): List<Tournament> =
    listOf(
        Tournament(
            id = "t1",
            title = "SMILY SHOWDOWN",
            totalPrizePool = 10000,
            participantsLabel = "32 Playing",
            scheduleLabel = "Dec 4th • 6:00-6:30 pm",
            status = TournamentStatus.Live(Clock.System.now() + 10.minutes),
            participationState = TournamentParticipationState.RegistrationRequired(20),
            prizeBreakdown = samplePrizeRows(),
        ),
        Tournament(
            id = "t2",
            title = "SMILY SHOWDOWN",
            totalPrizePool = 10000,
            participantsLabel = "15 Registered",
            scheduleLabel = "Dec 5th • 6:00-6:30 pm",
            status = TournamentStatus.Upcoming(Clock.System.now() + 10.minutes),
            participationState = TournamentParticipationState.Registered,
            prizeBreakdown = samplePrizeRows(),
        ),
        Tournament(
            id = "t3",
            title = "SMILY SHOWDOWN",
            totalPrizePool = 10000,
            participantsLabel = "15 Participants",
            scheduleLabel = "Dec 5th • 6:00-6:30 pm",
            status = TournamentStatus.Ended,
            participationState = TournamentParticipationState.Registered,
            prizeBreakdown = samplePrizeRows(),
        ),
    )

internal fun sampleHistoryTournaments(): List<Tournament> =
    listOf(
        Tournament(
            id = "h1",
            title = "SMILY SHOWDOWN",
            totalPrizePool = 10000,
            participantsLabel = "15 Participants",
            scheduleLabel = "Dec 5th • 6:00-6:30 pm",
            status = TournamentStatus.Ended,
            participationState = TournamentParticipationState.Registered,
            prizeBreakdown = samplePrizeRows(),
        ),
    )

private fun samplePrizeRows(): List<PrizeBreakdownRow> =
    listOf(
        PrizeBreakdownRow(rankLabel = "1st Place", amountLabel = "₹10,000 worth of"),
        PrizeBreakdownRow(rankLabel = "2nd Place", amountLabel = "₹5,000 in"),
        PrizeBreakdownRow(rankLabel = "3rd Place", amountLabel = "₹4,000 in"),
        PrizeBreakdownRow(rankLabel = "4th Place", amountLabel = "₹3,000 in"),
        PrizeBreakdownRow(rankLabel = "5th Place", amountLabel = "₹2,000 in"),
        PrizeBreakdownRow(rankLabel = "6th Place", amountLabel = "₹1,000 in"),
        PrizeBreakdownRow(rankLabel = "7th Place", amountLabel = "₹500 in"),
        PrizeBreakdownRow(rankLabel = "8th Place", amountLabel = "₹400 in"),
        PrizeBreakdownRow(rankLabel = "9th Place", amountLabel = "₹300 in"),
        PrizeBreakdownRow(rankLabel = "10th Place", amountLabel = "₹100 in"),
    )
