package com.yral.shared.features.game.domain.models

sealed class CastHotOrNotVoteResponse {
    data class Success(
        val outcome: String,
        val coins: Long,
        val coinDelta: Int,
    ) : CastHotOrNotVoteResponse()

    data class Error(
        val error: CastHotOrNotVoteError,
    ) : CastHotOrNotVoteResponse()
}

data class CastHotOrNotVoteError(
    val code: CastHotOrNotVoteErrorCodes,
    val message: String,
)

enum class CastHotOrNotVoteErrorCodes {
    INSUFFICIENT_COINS,
    MISSING_ID_TOKEN,
    APPCHECK_INVALID,
    INVALID_VOTE,
    INVALID_VIDEO_ID,
    INVALID_PRINCIPAL_ID,
    DUPLICATE_VOTE,
    ID_TOKEN_INVALID,
    FIRESTORE_ERROR,
    INTERNAL,
}

fun CastHotOrNotVoteResponse.toVoteResult(): VoteResult =
    when (this) {
        is CastHotOrNotVoteResponse.Success -> {
            VoteResult(
                coinDelta = this.coinDelta,
                errorMessage = "",
            )
        }
        is CastHotOrNotVoteResponse.Error -> {
            VoteResult(
                coinDelta = 0,
                errorMessage = this.error.message,
            )
        }
    }
