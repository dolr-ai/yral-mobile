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
            startEpochMs = Clock.System.now().toEpochMilliseconds(),
            endEpochMs = (Clock.System.now() + 10.minutes).toEpochMilliseconds(),
            entryCost = 20,
            entryCostCredits = 1,
            isRegistered = false,
            userDiamonds = 0,
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
            startEpochMs = Clock.System.now().toEpochMilliseconds(),
            endEpochMs = (Clock.System.now() + 10.minutes).toEpochMilliseconds(),
            entryCost = 20,
            entryCostCredits = 1,
            isRegistered = true,
            userDiamonds = 30,
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
            startEpochMs = (Clock.System.now() - 20.minutes).toEpochMilliseconds(),
            endEpochMs = (Clock.System.now() - 10.minutes).toEpochMilliseconds(),
            entryCost = 20,
            entryCostCredits = 1,
            isRegistered = true,
            userDiamonds = 30,
        ),
    )

@OptIn(ExperimentalTime::class)
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
            startEpochMs = (Clock.System.now() - 20.minutes).toEpochMilliseconds(),
            endEpochMs = (Clock.System.now() - 10.minutes).toEpochMilliseconds(),
            entryCost = 20,
            entryCostCredits = 1,
            isRegistered = true,
            userDiamonds = 30,
        ),
    )

private fun samplePrizeRows(): List<PrizeBreakdownRow> =
    listOf(
        PrizeBreakdownRow(rank = 1, amount = 10000),
        PrizeBreakdownRow(rank = 2, amount = 5000),
        PrizeBreakdownRow(rank = 3, amount = 4000),
        PrizeBreakdownRow(rank = 4, amount = 3000),
        PrizeBreakdownRow(rank = 5, amount = 2000),
        PrizeBreakdownRow(rank = 6, amount = 1000),
        PrizeBreakdownRow(rank = 7, amount = 500),
        PrizeBreakdownRow(rank = 8, amount = 400),
        PrizeBreakdownRow(rank = 9, amount = 300),
        PrizeBreakdownRow(rank = 10, amount = 100),
    )
