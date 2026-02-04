package com.yral.shared.features.tournament.domain.model

import com.yral.shared.features.tournament.data.models.MyTournamentsRequestDto
import com.yral.shared.features.tournament.data.models.RegisterTournamentRequestDto
import com.yral.shared.features.tournament.data.models.TournamentLeaderboardRequestDto
import com.yral.shared.features.tournament.data.models.TournamentListRequestDto
import com.yral.shared.features.tournament.data.models.TournamentStatusRequestDto
import com.yral.shared.features.tournament.data.models.TournamentVoteRequestDto

data class GetTournamentsRequest(
    val date: String? = null,
    val status: String? = null,
    val principalId: String? = null,
    val tournamentId: String? = null,
)

fun GetTournamentsRequest.toDto(): TournamentListRequestDto =
    TournamentListRequestDto(
        date = date,
        status = status,
        principalId = principalId,
        tournamentId = tournamentId,
    )

data class GetTournamentStatusRequest(
    val tournamentId: String,
)

fun GetTournamentStatusRequest.toDto(): TournamentStatusRequestDto =
    TournamentStatusRequestDto(
        tournamentId = tournamentId,
    )

data class RegisterForTournamentRequest(
    val tournamentId: String,
    val principalId: String,
    val isPro: Boolean = false,
)

fun RegisterForTournamentRequest.toDto(): RegisterTournamentRequestDto =
    RegisterTournamentRequestDto(
        tournamentId = tournamentId,
        principalId = principalId,
        isPro = isPro,
    )

data class GetMyTournamentsRequest(
    val principalId: String,
)

fun GetMyTournamentsRequest.toDto(): MyTournamentsRequestDto =
    MyTournamentsRequestDto(
        principalId = principalId,
    )

data class CastTournamentVoteRequest(
    val tournamentId: String,
    val principalId: String,
    val videoId: String,
    val smileyId: String,
)

fun CastTournamentVoteRequest.toDto(): TournamentVoteRequestDto =
    TournamentVoteRequestDto(
        tournamentId = tournamentId,
        principalId = principalId,
        videoId = videoId,
        smileyId = smileyId,
    )

data class GetTournamentLeaderboardRequest(
    val tournamentId: String,
    val principalId: String,
)

fun GetTournamentLeaderboardRequest.toDto(): TournamentLeaderboardRequestDto =
    TournamentLeaderboardRequestDto(
        tournamentId = tournamentId,
        principalId = principalId,
    )
