package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.RegistrationResult
import com.yral.shared.features.tournament.domain.model.TournamentError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class RegisterTournamentResponseDto {
    @Serializable
    data class Success(
        @SerialName("status")
        val status: String,
        @SerialName("tournament_id")
        val tournamentId: String,
        @SerialName("coins_paid")
        val coinsPaid: Int,
        @SerialName("coins_remaining")
        val coinsRemaining: Int,
    ) : RegisterTournamentResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : RegisterTournamentResponseDto()
}

fun RegisterTournamentResponseDto.toRegistrationResult(): Result<RegistrationResult, TournamentError> =
    when (this) {
        is RegisterTournamentResponseDto.Success -> {
            Ok(
                RegistrationResult(
                    status = status,
                    tournamentId = tournamentId,
                    coinsPaid = coinsPaid,
                    coinsRemaining = coinsRemaining,
                ),
            )
        }
        is RegisterTournamentResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }
