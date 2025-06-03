package com.yral.shared.features.game.domain.models

sealed class CastVoteResponse {
    data class Success(
        val outcome: String,
        val coins: Long,
        val coinDelta: Int,
    ) : CastVoteResponse()

    data class Error(
        val error: CastVoteError,
    ) : CastVoteResponse()
}

data class CastVoteError(
    val code: CastVoteErrorCodes,
    val message: String,
)

enum class CastVoteErrorCodes {
    INSUFFICIENT_COINS,
    MISSING_ID_TOKEN,
    APPCHECK_INVALID,
    SMILEY_NOT_ALLOWED,
    DUPLICATE_VOTE,
    ID_TOKEN_INVALID,
    FIRESTORE_ERROR,
    INTERNAL,
}
