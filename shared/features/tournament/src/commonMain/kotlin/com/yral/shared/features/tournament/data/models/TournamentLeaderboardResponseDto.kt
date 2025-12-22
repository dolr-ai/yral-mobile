package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.LeaderboardRow
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentLeaderboard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TournamentLeaderboardResponseDto {
    @Serializable
    data class Success(
        @SerialName("tournament_id")
        val tournamentId: String,
        @SerialName("status")
        val status: String,
        @SerialName("top_rows")
        val topRows: List<LeaderboardRowDto>,
        @SerialName("user_row")
        val userRow: LeaderboardRowDto? = null,
        @SerialName("prize_map")
        val prizeMap: Map<String, Int>,
    ) : TournamentLeaderboardResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : TournamentLeaderboardResponseDto()
}

@Serializable
data class LeaderboardRowDto(
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("wins")
    val wins: Int,
    @SerialName("losses")
    val losses: Int,
    @SerialName("position")
    val position: Int,
    @SerialName("prize")
    val prize: Int? = null,
)

fun TournamentLeaderboardResponseDto.toTournamentLeaderboard(): Result<TournamentLeaderboard, TournamentError> =
    when (this) {
        is TournamentLeaderboardResponseDto.Success -> {
            Ok(
                TournamentLeaderboard(
                    tournamentId = tournamentId,
                    status = status,
                    topRows = topRows.map { it.toLeaderboardRow() },
                    userRow = userRow?.toLeaderboardRow(),
                    prizeMap =
                        prizeMap
                            .mapNotNull { (key, value) ->
                                key.toIntOrNull()?.let { it to value }
                            }.toMap(),
                ),
            )
        }
        is TournamentLeaderboardResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }

fun LeaderboardRowDto.toLeaderboardRow(): LeaderboardRow =
    LeaderboardRow(
        principalId = principalId,
        username = username,
        wins = wins,
        losses = losses,
        position = position,
        prize = prize,
    )
