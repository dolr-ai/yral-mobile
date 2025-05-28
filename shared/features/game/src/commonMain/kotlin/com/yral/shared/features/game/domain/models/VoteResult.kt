package com.yral.shared.features.game.domain.models

data class VoteResult(
    val coinDelta: Int,
    val errorMessage: String,
)

fun CastVoteResponse.toVoteResult(): VoteResult =
    when (this) {
        is CastVoteResponse.Success -> {
            VoteResult(
                coinDelta = this.coinDelta,
                errorMessage = "",
            )
        }
        is CastVoteResponse.Error -> {
            VoteResult(
                coinDelta = 0,
                errorMessage = this.error.message,
            )
        }
    }
