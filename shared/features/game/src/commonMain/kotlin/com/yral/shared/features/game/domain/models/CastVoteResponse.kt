package com.yral.shared.features.game.domain.models

data class CastVoteResponse(
    val outcome: String,
    val coins: Long,
    val coinDelta: Int,
    val error: CastVoteError? = null,
)

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
