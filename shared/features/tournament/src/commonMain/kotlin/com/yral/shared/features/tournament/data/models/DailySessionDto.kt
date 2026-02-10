package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.DailySessionResult
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailySessionRequestDto(
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("principal_id")
    val principalId: String,
)

@Serializable
sealed class DailySessionResponseDto {
    @Serializable
    data class Success(
        @SerialName("remaining_time_ms")
        val remainingTimeMs: Long,
        @SerialName("diamonds")
        val diamonds: Int,
        @SerialName("wins")
        val wins: Int = 0,
        @SerialName("losses")
        val losses: Int = 0,
        @SerialName("position")
        val position: Int = 0,
        @SerialName("time_spent_ms")
        val timeSpentMs: Long = 0,
    ) : DailySessionResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : DailySessionResponseDto()
}

fun DailySessionResponseDto.toDailySessionResult(): Result<DailySessionResult, TournamentError> =
    when (this) {
        is DailySessionResponseDto.Success ->
            Ok(
                DailySessionResult(
                    remainingTimeMs = remainingTimeMs,
                    diamonds = diamonds,
                    wins = wins,
                    losses = losses,
                    position = position,
                    timeSpentMs = timeSpentMs,
                ),
            )
        is DailySessionResponseDto.Error ->
            Err(
                TournamentError(
                    code = TournamentErrorCodes.fromCode(error.code),
                    message = error.message,
                ),
            )
    }
