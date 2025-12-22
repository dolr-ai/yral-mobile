package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentStatusData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TournamentStatusResponseDto {
    @Serializable
    data class Success(
        @SerialName("tournament_id")
        val tournamentId: String,
        @SerialName("status")
        val status: String,
        @SerialName("participant_count")
        val participantCount: Int,
        @SerialName("time_left_ms")
        val timeLeftMs: Long? = null,
    ) : TournamentStatusResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : TournamentStatusResponseDto()
}

fun TournamentStatusResponseDto.toTournamentStatusData(): Result<TournamentStatusData, TournamentError> =
    when (this) {
        is TournamentStatusResponseDto.Success -> {
            Ok(
                TournamentStatusData(
                    tournamentId = tournamentId,
                    status = status,
                    participantCount = participantCount,
                    timeLeftMs = timeLeftMs,
                ),
            )
        }
        is TournamentStatusResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }
