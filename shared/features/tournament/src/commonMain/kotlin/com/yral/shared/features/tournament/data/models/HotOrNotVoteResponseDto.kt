package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.HotOrNotVoteResult
import com.yral.shared.features.tournament.domain.model.TournamentError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class HotOrNotVoteResponseDto {
    @Serializable
    data class Success(
        @SerialName("outcome")
        val outcome: String,
        @SerialName("vote")
        val vote: String,
        @SerialName("ai_verdict")
        val aiVerdict: String,
        @SerialName("diamonds")
        val diamonds: Int,
        @SerialName("diamond_delta")
        val diamondDelta: Int,
        @SerialName("wins")
        val wins: Int = 0,
        @SerialName("losses")
        val losses: Int = 0,
        @SerialName("position")
        val position: Int = 0,
    ) : HotOrNotVoteResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : HotOrNotVoteResponseDto()
}

fun HotOrNotVoteResponseDto.toVoteResult(): Result<HotOrNotVoteResult, TournamentError> =
    when (this) {
        is HotOrNotVoteResponseDto.Success -> {
            Ok(
                HotOrNotVoteResult(
                    outcome = outcome,
                    vote = vote,
                    aiVerdict = aiVerdict,
                    diamonds = diamonds,
                    diamondDelta = diamondDelta,
                    wins = wins,
                    losses = losses,
                    position = position,
                ),
            )
        }
        is HotOrNotVoteResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }
