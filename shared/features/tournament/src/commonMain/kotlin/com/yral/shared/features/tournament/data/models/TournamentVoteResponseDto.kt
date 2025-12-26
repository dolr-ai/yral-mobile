package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.VoteOutcome
import com.yral.shared.features.tournament.domain.model.VoteResult
import com.yral.shared.features.tournament.domain.model.VotedSmiley
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TournamentVoteResponseDto {
    @Serializable
    data class Success(
        @SerialName("outcome")
        val outcome: String,
        @SerialName("smiley")
        val smiley: SmileyDto,
        @SerialName("tournament_wins")
        val tournamentWins: Int,
        @SerialName("tournament_losses")
        val tournamentLosses: Int,
        @SerialName("diamonds")
        val diamonds: Int,
        @SerialName("position")
        val position: Int,
        @SerialName("diamond_delta")
        val diamondDelta: Int? = null,
    ) : TournamentVoteResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : TournamentVoteResponseDto()
}

@Serializable
data class SmileyDto(
    @SerialName("id")
    val id: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("click_animation")
    val clickAnimation: String? = null,
    @SerialName("image_fallback")
    val imageFallback: String? = null,
)

fun TournamentVoteResponseDto.toVoteResult(): Result<VoteResult, TournamentError> =
    when (this) {
        is TournamentVoteResponseDto.Success -> {
            Ok(
                VoteResult(
                    outcome = VoteOutcome.fromString(outcome),
                    smiley =
                        VotedSmiley(
                            id = smiley.id,
                            imageUrl = smiley.imageUrl,
                            isActive = smiley.isActive,
                            clickAnimation = smiley.clickAnimation,
                            imageFallback = smiley.imageFallback,
                        ),
                    tournamentWins = tournamentWins,
                    tournamentLosses = tournamentLosses,
                    diamonds = diamonds,
                    position = position,
                    diamondDelta = diamondDelta,
                ),
            )
        }
        is TournamentVoteResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }
